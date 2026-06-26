package com.campuscafe.backend.order.service;

import com.campuscafe.backend.domain.merchant.Merchant;
import com.campuscafe.backend.domain.merchant.MerchantSetting;
import com.campuscafe.backend.domain.order.Order;
import com.campuscafe.backend.domain.order.OrderItem;
import com.campuscafe.backend.domain.order.enums.OrderSource;
import com.campuscafe.backend.domain.order.enums.OrderStatus;
import com.campuscafe.backend.domain.notification.Notification;
import com.campuscafe.backend.domain.notification.enums.NotificationType;
import com.campuscafe.backend.domain.product.Product;
import com.campuscafe.backend.domain.user.User;
import com.campuscafe.backend.exception.*;
import com.campuscafe.backend.order.dto.*;
import com.campuscafe.backend.order.mapper.OrderMapper;
import com.campuscafe.backend.order.repository.OrderItemRepository;
import com.campuscafe.backend.order.repository.OrderRepository;
import com.campuscafe.backend.order.specification.OrderSpecification;
import com.campuscafe.backend.product.repository.ProductRepository;
import com.campuscafe.backend.repository.MerchantRepository;
import com.campuscafe.backend.repository.NotificationRepository;
import com.campuscafe.backend.repository.UserRepository;
import com.campuscafe.backend.discount.repository.DiscountRepository;
import com.campuscafe.backend.domain.discount.Discount;
import com.campuscafe.backend.domain.discount.enums.DiscountType;
import com.campuscafe.backend.domain.order.enums.OrderPriority;
import com.campuscafe.backend.domain.order.enums.PaymentMethod;
import com.campuscafe.backend.domain.merchant.enums.ShopStatus;
import com.campuscafe.backend.repository.MerchantSettingRepository;
import com.campuscafe.backend.domain.product.ProductRecipe;
import com.campuscafe.backend.domain.inventory.InventoryItem;
import com.campuscafe.backend.domain.inventory.InventoryTransaction;
import com.campuscafe.backend.domain.inventory.enums.InventoryTransactionType;
import com.campuscafe.backend.product.repository.ProductRecipeRepository;
import com.campuscafe.backend.inventory.repository.InventoryItemRepository;
import com.campuscafe.backend.inventory.repository.InventoryTransactionRepository;
import com.campuscafe.backend.security.service.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final MerchantRepository merchantRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final DiscountRepository discountRepository;
    private final ProductRecipeRepository productRecipeRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final MerchantSettingRepository merchantSettingRepository;

    private final OrderMapper orderMapper;

    private CustomUserDetails getAuthenticatedUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            throw new AccessDeniedException("User is not authenticated");
        }
        return (CustomUserDetails) authentication.getPrincipal();
    }

    private User getCurrentUserEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AccessDeniedException("Authenticated user details not found"));
    }

    public OrderResponse createOrder(CreateOrderRequest request) {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new InvalidOrderRequestException("Order must contain at least one item");
        }

        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new AccessDeniedException("Merchant not found"));

        if (merchant.getShopStatus() == ShopStatus.CLOSED) {
            throw new InvalidOrderRequestException("Cannot place order because the shop is CLOSED");
        }

        OrderPriority priority = OrderPriority.MEDIUM;
        if (request.getPriority() != null) {
            try {
                priority = OrderPriority.valueOf(request.getPriority().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new InvalidOrderRequestException("Invalid priority value: " + request.getPriority());
            }
        }

        PaymentMethod paymentMethod = PaymentMethod.CASH;
        if (request.getPaymentMethod() != null) {
            try {
                paymentMethod = PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new InvalidOrderRequestException("Invalid payment method: " + request.getPaymentMethod());
            }
        }

        Order order = Order.builder()
                .merchant(merchant)
                .status(OrderStatus.NEW)
                .priority(priority)
                .source(request.getSource() != null ? request.getSource() : OrderSource.OFFLINE)
                .paymentMethod(paymentMethod)
                .createdBy(getCurrentUserEntity(currentUser.getUserId()))
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemRequest itemReq : request.getItems()) {
            if (itemReq.getQuantity() == null || itemReq.getQuantity() <= 0) {
                throw new InvalidOrderRequestException("Quantity must be greater than zero");
            }

            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + itemReq.getProductId()));

            // Tenant Check
            if (!product.getMerchant().getId().equals(merchantId)) {
                throw new AccessDeniedException("You do not have access to this product");
            }

            // Availability Check
            if (!product.getAvailable()) {
                throw new ProductUnavailableException("Product '" + product.getName() + "' is currently unavailable");
            }

            BigDecimal itemSubtotal = product.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            subtotal = subtotal.add(itemSubtotal);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(itemReq.getQuantity())
                    .unitPrice(product.getPrice())
                    .subtotal(itemSubtotal)
                    .build();

            orderItems.add(orderItem);
        }

        // Apply Discount
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (request.getDiscountId() != null) {
            Discount discount = discountRepository.findByIdAndMerchantId(request.getDiscountId(), merchantId)
                    .orElseThrow(() -> new DiscountNotFoundException("Discount not found with id: " + request.getDiscountId()));

            if (!discount.getActive()) {
                throw new InactiveDiscountException("Discount is inactive: " + discount.getName());
            }

            if (discount.getDiscountType() == DiscountType.PERCENTAGE) {
                BigDecimal pct = discount.getValue().divide(BigDecimal.valueOf(100));
                discountAmount = subtotal.multiply(pct).setScale(2, java.math.RoundingMode.HALF_UP);
                if (discount.getMaxDiscount() != null && discount.getMaxDiscount().compareTo(BigDecimal.ZERO) > 0) {
                    if (discountAmount.compareTo(discount.getMaxDiscount()) > 0) {
                        discountAmount = discount.getMaxDiscount();
                    }
                }
            } else if (discount.getDiscountType() == DiscountType.FLAT) {
                discountAmount = discount.getValue();
                if (discountAmount.compareTo(subtotal) > 0) {
                    discountAmount = subtotal;
                }
            }
        }

        order.setItems(orderItems);
        order.setSubtotal(subtotal);
        order.setDiscountAmount(discountAmount);
        order.setFinalAmount(subtotal.subtract(discountAmount));

        // Generate Order Number
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Long sequenceVal = orderRepository.getNextOrderNumberSequence();
        String orderNumber = String.format("ORD-%s-%d", dateStr, sequenceVal);
        order.setOrderNumber(orderNumber);

        Order savedOrder = orderRepository.save(order);

        // Send NEW_ORDER Notification
        Notification notification = Notification.builder()
                .merchant(merchant)
                .type(NotificationType.NEW_ORDER)
                .title("New Order Placed: " + savedOrder.getOrderNumber())
                .message("A new order was placed. Order Number: " + savedOrder.getOrderNumber() + ", Total Amount: " + savedOrder.getFinalAmount())
                .readStatus(false)
                .build();
        notificationRepository.save(notification);

        return orderMapper.toResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrders(OrderStatus status, OrderSource source, String orderNumber, Instant startDate, Instant endDate, Pageable pageable) {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        Specification<Order> spec = Specification.where(OrderSpecification.withMerchantId(merchantId));

        if (status != null) {
            spec = spec.and(OrderSpecification.withStatus(status));
        }
        if (source != null) {
            spec = spec.and(OrderSpecification.withSource(source));
        }
        if (orderNumber != null) {
            spec = spec.and(OrderSpecification.withOrderNumber(orderNumber));
        }
        if (startDate != null || endDate != null) {
            spec = spec.and(OrderSpecification.withDateRange(startDate, endDate));
        }

        Page<Order> orders = orderRepository.findAll(spec, pageable);
        return orders.map(orderMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public OrderDetailsResponse getOrderById(Long id) {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));

        if (!order.getMerchant().getId().equals(currentUser.getMerchantId())) {
            throw new AccessDeniedException("You do not have access to this order");
        }

        return orderMapper.toDetailsResponse(order);
    }

    public OrderResponse updateOrderStatus(Long id, UpdateOrderStatusRequest request) {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));

        if (!order.getMerchant().getId().equals(merchantId)) {
            throw new AccessDeniedException("You do not have access to this order");
        }

        OrderStatus current = order.getStatus();
        OrderStatus target;
        try {
            target = OrderStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidOrderStatusException("Invalid status value: " + request.getStatus());
        }

        boolean isValid = false;
        if (current == OrderStatus.NEW) {
            isValid = (target == OrderStatus.PREPARING || target == OrderStatus.CANCELLED);
        } else if (current == OrderStatus.PREPARING) {
            isValid = (target == OrderStatus.READY || target == OrderStatus.CANCELLED);
        } else if (current == OrderStatus.READY) {
            isValid = (target == OrderStatus.COMPLETED || target == OrderStatus.CANCELLED);
        }

        if (!isValid) {
            throw new InvalidOrderTransitionException("Cannot transition order from " + current + " to " + target);
        }

        if (target == OrderStatus.COMPLETED) {
            User userEntity = getCurrentUserEntity(currentUser.getUserId());
            deductInventoryForOrder(order, userEntity);
        }

        order.setStatus(target);
        Order updatedOrder = orderRepository.save(order);
        return orderMapper.toResponse(updatedOrder);
    }

    @Transactional(readOnly = true)
    public OrderBoardResponse getOrderBoard() {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        List<Order> orders = orderRepository.findActiveOrdersSorted(merchantId, List.of(OrderStatus.NEW, OrderStatus.PREPARING, OrderStatus.READY));

        List<OrderResponse> newOrdersList = new ArrayList<>();
        List<OrderResponse> preparingList = new ArrayList<>();
        List<OrderResponse> readyList = new ArrayList<>();

        for (Order order : orders) {
            OrderResponse response = orderMapper.toResponse(order);
            if (order.getStatus() == OrderStatus.NEW) {
                newOrdersList.add(response);
            } else if (order.getStatus() == OrderStatus.PREPARING) {
                preparingList.add(response);
            } else if (order.getStatus() == OrderStatus.READY) {
                readyList.add(response);
            }
        }

        return OrderBoardResponse.builder()
                .newOrders(newOrdersList)
                .preparing(preparingList)
                .ready(readyList)
                .build();
    }

    public OrderResponse updateOrderPriority(Long id, UpdateOrderPriorityRequest request) {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));

        if (!order.getMerchant().getId().equals(merchantId)) {
            throw new AccessDeniedException("You do not have access to this order");
        }

        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new InvalidOrderStatusException("Cannot change priority of completed or cancelled orders");
        }

        OrderPriority priority;
        try {
            priority = OrderPriority.valueOf(request.getPriority().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidOrderStatusException("Invalid priority value: " + request.getPriority());
        }

        order.setPriority(priority);
        Order updated = orderRepository.save(order);
        return orderMapper.toResponse(updated);
    }

    private void deductInventoryForOrder(Order order, User userEntity) {
        java.util.Map<InventoryItem, BigDecimal> requiredDeductions = new java.util.HashMap<>();

        for (OrderItem item : order.getItems()) {
            List<ProductRecipe> recipes = productRecipeRepository.findByProductId(item.getProduct().getId());
            for (ProductRecipe recipe : recipes) {
                InventoryItem ingredient = recipe.getInventoryItem();
                BigDecimal perItemQty = recipe.getQuantityRequired();
                BigDecimal totalQty = perItemQty.multiply(BigDecimal.valueOf(item.getQuantity()));
                
                requiredDeductions.put(ingredient, requiredDeductions.getOrDefault(ingredient, BigDecimal.ZERO).add(totalQty));
            }
        }

        // Validate stock sufficiency for all ingredients first
        for (java.util.Map.Entry<InventoryItem, BigDecimal> entry : requiredDeductions.entrySet()) {
            InventoryItem ingredient = entry.getKey();
            BigDecimal req = entry.getValue();

            InventoryItem dbItem = inventoryItemRepository.findById(ingredient.getId())
                    .orElseThrow(() -> new InventoryItemNotFoundException("Inventory item not found: " + ingredient.getName()));

            if (dbItem.getCurrentStock().compareTo(req) < 0) {
                throw new InsufficientInventoryException("Insufficient inventory for item: " + dbItem.getName() +
                        ". Available: " + dbItem.getCurrentStock() + ", Required: " + req);
            }
        }

        // Deduct stock and save transaction logs
        for (java.util.Map.Entry<InventoryItem, BigDecimal> entry : requiredDeductions.entrySet()) {
            InventoryItem ingredient = entry.getKey();
            BigDecimal req = entry.getValue();

            InventoryItem dbItem = inventoryItemRepository.findById(ingredient.getId()).get();
            dbItem.setCurrentStock(dbItem.getCurrentStock().subtract(req));
            InventoryItem savedItem = inventoryItemRepository.save(dbItem);

            InventoryTransaction transaction = InventoryTransaction.builder()
                    .inventoryItem(savedItem)
                    .type(InventoryTransactionType.STOCK_OUT)
                    .quantity(req)
                    .remarks("Automatic deduction for Order: " + order.getOrderNumber())
                    .createdBy(userEntity)
                    .build();
            inventoryTransactionRepository.save(transaction);

            checkLowStockAndNotify(savedItem, order.getMerchant());
        }
    }

    private void checkLowStockAndNotify(InventoryItem item, Merchant merchant) {
        if (item.getCurrentStock().compareTo(item.getMinStock()) <= 0) {
            String title = "Low Stock Alert: " + item.getName();
            boolean exists = notificationRepository.existsByMerchantIdAndTypeAndTitleAndReadStatus(
                    merchant.getId(), NotificationType.LOW_STOCK, title, false
            );
            if (!exists) {
                Notification notification = Notification.builder()
                        .merchant(merchant)
                        .type(NotificationType.LOW_STOCK)
                        .title(title)
                        .message(String.format("Inventory item '%s' is running low. Current stock: %s %s (Min threshold: %s %s).",
                                item.getName(), item.getCurrentStock(), item.getUnit(), item.getMinStock(), item.getUnit()))
                        .readStatus(false)
                        .build();
                notificationRepository.save(notification);
            }
        }
    }

    @Transactional(readOnly = true)
    public OrderDashboardResponse getDashboardMetrics() {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        LocalDate today = LocalDate.now();
        Instant startOfDay = today.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endOfDay = today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        long todayOrders = orderRepository.countByMerchantIdAndCreatedAtBetween(merchantId, startOfDay, endOfDay);
        BigDecimal todayRevenue = orderRepository.sumRevenueByMerchantIdAndStatusAndCreatedAtBetween(merchantId, OrderStatus.COMPLETED, startOfDay, endOfDay);
        if (todayRevenue == null) {
            todayRevenue = BigDecimal.ZERO.setScale(2);
        }

        long newOrders = orderRepository.countByMerchantIdAndStatus(merchantId, OrderStatus.NEW);
        long readyOrders = orderRepository.countByMerchantIdAndStatus(merchantId, OrderStatus.READY);

        return OrderDashboardResponse.builder()
                .todayOrders(todayOrders)
                .todayRevenue(todayRevenue)
                .newOrders(newOrders)
                .readyOrders(readyOrders)
                .build();
    }

    public String getReceiptText(Long orderId) {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));

        if (!order.getMerchant().getId().equals(currentUser.getMerchantId())) {
            throw new AccessDeniedException("You do not have access to this order");
        }

        MerchantSetting setting = merchantSettingRepository.findByMerchantId(order.getMerchant().getId())
                .orElseThrow(() -> new MerchantSettingsNotFoundException("Settings not found"));

        int width = setting.getPrinterSize() == com.campuscafe.backend.domain.merchant.enums.PrinterSize.SIZE_58MM ? 32 : 48;

        StringBuilder sb = new StringBuilder();
        
        // Helper to center text
        java.util.function.Function<String, String> center = (text) -> {
            if (text == null) text = "";
            if (text.length() >= width) return text.substring(0, width);
            int spaces = (width - text.length()) / 2;
            return " ".repeat(spaces) + text;
        };

        String singleLine = "-".repeat(width);
        String doubleLine = "=".repeat(width);

        sb.append(doubleLine).append("\n");
        sb.append(center.apply("TAX INVOICE")).append("\n");
        sb.append(center.apply(setting.getBusinessName().toUpperCase())).append("\n");
        if (setting.getAddress() != null && !setting.getAddress().isBlank()) {
            sb.append(center.apply(setting.getAddress())).append("\n");
        }
        sb.append(doubleLine).append("\n");
        
        java.time.LocalDateTime ldt = java.time.LocalDateTime.ofInstant(order.getCreatedAt(), java.time.ZoneId.systemDefault());
        String formattedDate = ldt.format(java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm"));

        sb.append("Bill No : ").append(order.getOrderNumber()).append("\n");
        sb.append("Date    : ").append(formattedDate).append("\n");
        sb.append("Cashier : ").append(order.getCreatedBy().getName()).append("\n");
        sb.append(doubleLine).append("\n");
        
        // Product List header
        if (width == 32) {
            sb.append(String.format("%-15s %3s %11s", "Item", "Qty", "Amount")).append("\n");
        } else {
            sb.append(String.format("%-25s %4s %8s %9s", "Item", "Qty", "Rate", "Amount")).append("\n");
        }
        sb.append(singleLine).append("\n");

        for (OrderItem item : order.getItems()) {
            String name = item.getProduct().getName();
            if (name.length() > (width == 32 ? 15 : 25)) {
                name = name.substring(0, (width == 32 ? 12 : 22)) + "...";
            }
            if (width == 32) {
                sb.append(String.format("%-15s %3d %11.2f", name, item.getQuantity(), item.getSubtotal())).append("\n");
            } else {
                sb.append(String.format("%-25s %4d %8.2f %9.2f", name, item.getQuantity(), item.getUnitPrice(), item.getSubtotal())).append("\n");
            }
        }
        sb.append(singleLine).append("\n");
        
        // Totals
        java.util.function.BiConsumer<String, BigDecimal> addTotalLine = (label, val) -> {
            String valStr = String.format("%.2f", val);
            int pad = width - label.length() - valStr.length();
            if (pad > 0) {
                sb.append(label).append(" ".repeat(pad)).append(valStr).append("\n");
            } else {
                sb.append(label).append(" ").append(valStr).append("\n");
            }
        };

        addTotalLine.accept("SUBTOTAL:", order.getSubtotal());
        if (order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            addTotalLine.accept("DISCOUNT:", order.getDiscountAmount());
        }
        
        BigDecimal finalAmount = order.getFinalAmount();
        BigDecimal divisor = BigDecimal.valueOf(1.05);
        BigDecimal taxableAmount = finalAmount.divide(divisor, 2, java.math.RoundingMode.HALF_UP);
        BigDecimal cgst = taxableAmount.multiply(BigDecimal.valueOf(0.025)).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal sgst = cgst;

        sb.append(singleLine).append("\n");
        addTotalLine.accept("TAXABLE VALUE:", taxableAmount);
        addTotalLine.accept("CGST (2.5%):", cgst);
        addTotalLine.accept("SGST (2.5%):", sgst);
        sb.append(singleLine).append("\n");
        addTotalLine.accept("GRAND TOTAL:", finalAmount);
        sb.append(doubleLine).append("\n");
        sb.append("Payment Mode: ").append(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : "N/A").append("\n");
        sb.append(doubleLine).append("\n");
        sb.append(center.apply("Thank you for your visit!")).append("\n");
        sb.append(center.apply("Please visit us again!")).append("\n");
        sb.append(doubleLine).append("\n");

        return sb.toString();
    }
}

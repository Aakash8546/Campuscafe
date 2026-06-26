package com.campuscafe.backend.order.service;

import com.campuscafe.backend.discount.repository.DiscountRepository;
import com.campuscafe.backend.domain.discount.Discount;
import com.campuscafe.backend.domain.discount.enums.DiscountType;
import com.campuscafe.backend.domain.merchant.Merchant;
import com.campuscafe.backend.domain.notification.Notification;
import com.campuscafe.backend.domain.order.Order;
import com.campuscafe.backend.domain.order.enums.OrderSource;
import com.campuscafe.backend.domain.order.enums.OrderStatus;
import com.campuscafe.backend.domain.product.Product;
import com.campuscafe.backend.domain.user.Role;
import com.campuscafe.backend.domain.user.User;
import com.campuscafe.backend.exception.*;
import com.campuscafe.backend.order.dto.*;
import com.campuscafe.backend.order.mapper.OrderMapper;
import com.campuscafe.backend.order.repository.OrderItemRepository;
import com.campuscafe.backend.order.repository.OrderRepository;
import com.campuscafe.backend.product.repository.ProductRepository;
import com.campuscafe.backend.repository.MerchantRepository;
import com.campuscafe.backend.repository.NotificationRepository;
import com.campuscafe.backend.repository.UserRepository;
import com.campuscafe.backend.security.service.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscountCalculationTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private DiscountRepository discountRepository;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderService orderService;

    private Merchant merchant;
    private User adminUser;
    private Product product;
    private Discount percentageDiscount;
    private Discount flatDiscount;
    private Discount cappedDiscount;

    @BeforeEach
    void setUp() {
        merchant = Merchant.builder().cafeName("Merchant A").email("merchantA@test.com").verified(true).build();
        merchant.setId(1L);

        Role adminRole = Role.builder().name("ADMIN").build();
        adminUser = User.builder().email("admin@merchantA.com").role(adminRole).merchant(merchant).active(true).build();
        adminUser.setId(1L);

        product = Product.builder().name("Item").price(new BigDecimal("100.00")).merchant(merchant).available(true).build();
        product.setId(10L);

        percentageDiscount = Discount.builder()
                .merchant(merchant)
                .name("10% OFF")
                .discountType(DiscountType.PERCENTAGE)
                .value(new BigDecimal("10.00"))
                .active(true)
                .build();
        percentageDiscount.setId(100L);

        flatDiscount = Discount.builder()
                .merchant(merchant)
                .name("Flat 200")
                .discountType(DiscountType.FLAT)
                .value(new BigDecimal("200.00"))
                .active(true)
                .build();
        flatDiscount.setId(200L);

        cappedDiscount = Discount.builder()
                .merchant(merchant)
                .name("10% OFF max 50")
                .discountType(DiscountType.PERCENTAGE)
                .value(new BigDecimal("10.00"))
                .maxDiscount(new BigDecimal("50.00"))
                .active(true)
                .build();
        cappedDiscount.setId(300L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setupSecurityContext(User user) {
        CustomUserDetails userDetails = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void testCreateOrder_WithPercentageDiscount_CalculatesCorrectly() {
        setupSecurityContext(adminUser);

        OrderItemRequest itemReq = OrderItemRequest.builder().productId(10L).quantity(10).build(); // Subtotal = 1000.00
        CreateOrderRequest request = CreateOrderRequest.builder()
                .source(OrderSource.OFFLINE)
                .discountId(100L)
                .items(List.of(itemReq))
                .build();

        when(merchantRepository.findById(1L)).thenReturn(Optional.of(merchant));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(discountRepository.findByIdAndMerchantId(100L, 1L)).thenReturn(Optional.of(percentageDiscount));
        when(orderRepository.getNextOrderNumberSequence()).thenReturn(1001L);

        Order savedOrder = Order.builder()
                .merchant(merchant)
                .orderNumber("ORD-20260624-0001")
                .status(OrderStatus.NEW)
                .source(OrderSource.OFFLINE)
                .subtotal(new BigDecimal("1000.00"))
                .discountAmount(new BigDecimal("100.00"))
                .finalAmount(new BigDecimal("900.00"))
                .build();
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        when(orderMapper.toResponse(any(Order.class))).thenReturn(
                OrderResponse.builder()
                        .subtotal(new BigDecimal("1000.00"))
                        .discountAmount(new BigDecimal("100.00"))
                        .finalAmount(new BigDecimal("900.00"))
                        .build()
        );

        OrderResponse result = orderService.createOrder(request);

        assertNotNull(result);
        assertEquals(new BigDecimal("1000.00"), result.getSubtotal());
        assertEquals(new BigDecimal("100.00"), result.getDiscountAmount());
        assertEquals(new BigDecimal("900.00"), result.getFinalAmount());
    }

    @Test
    void testCreateOrder_WithCappedPercentageDiscount_EnforcesMaxDiscountLimit() {
        setupSecurityContext(adminUser);

        OrderItemRequest itemReq = OrderItemRequest.builder().productId(10L).quantity(10).build(); // Subtotal = 1000.00
        CreateOrderRequest request = CreateOrderRequest.builder()
                .source(OrderSource.OFFLINE)
                .discountId(300L)
                .items(List.of(itemReq))
                .build();

        when(merchantRepository.findById(1L)).thenReturn(Optional.of(merchant));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(discountRepository.findByIdAndMerchantId(300L, 1L)).thenReturn(Optional.of(cappedDiscount));
        when(orderRepository.getNextOrderNumberSequence()).thenReturn(1001L);

        Order savedOrder = Order.builder()
                .merchant(merchant)
                .orderNumber("ORD-20260624-0001")
                .status(OrderStatus.NEW)
                .source(OrderSource.OFFLINE)
                .subtotal(new BigDecimal("1000.00"))
                .discountAmount(new BigDecimal("50.00")) // Capped
                .finalAmount(new BigDecimal("950.00"))
                .build();
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        when(orderMapper.toResponse(any(Order.class))).thenReturn(
                OrderResponse.builder()
                        .subtotal(new BigDecimal("1000.00"))
                        .discountAmount(new BigDecimal("50.00"))
                        .finalAmount(new BigDecimal("950.00"))
                        .build()
        );

        OrderResponse result = orderService.createOrder(request);

        assertNotNull(result);
        assertEquals(new BigDecimal("1000.00"), result.getSubtotal());
        assertEquals(new BigDecimal("50.00"), result.getDiscountAmount());
        assertEquals(new BigDecimal("950.00"), result.getFinalAmount());
    }

    @Test
    void testCreateOrder_WithFlatDiscount_CalculatesCorrectly() {
        setupSecurityContext(adminUser);

        OrderItemRequest itemReq = OrderItemRequest.builder().productId(10L).quantity(10).build(); // Subtotal = 1000.00
        CreateOrderRequest request = CreateOrderRequest.builder()
                .source(OrderSource.OFFLINE)
                .discountId(200L)
                .items(List.of(itemReq))
                .build();

        when(merchantRepository.findById(1L)).thenReturn(Optional.of(merchant));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(discountRepository.findByIdAndMerchantId(200L, 1L)).thenReturn(Optional.of(flatDiscount));
        when(orderRepository.getNextOrderNumberSequence()).thenReturn(1001L);

        Order savedOrder = Order.builder()
                .merchant(merchant)
                .orderNumber("ORD-20260624-0001")
                .status(OrderStatus.NEW)
                .source(OrderSource.OFFLINE)
                .subtotal(new BigDecimal("1000.00"))
                .discountAmount(new BigDecimal("200.00"))
                .finalAmount(new BigDecimal("800.00"))
                .build();
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        when(orderMapper.toResponse(any(Order.class))).thenReturn(
                OrderResponse.builder()
                        .subtotal(new BigDecimal("1000.00"))
                        .discountAmount(new BigDecimal("200.00"))
                        .finalAmount(new BigDecimal("800.00"))
                        .build()
        );

        OrderResponse result = orderService.createOrder(request);

        assertNotNull(result);
        assertEquals(new BigDecimal("1000.00"), result.getSubtotal());
        assertEquals(new BigDecimal("200.00"), result.getDiscountAmount());
        assertEquals(new BigDecimal("800.00"), result.getFinalAmount());
    }

    @Test
    void testCreateOrder_FlatDiscountExceedsSubtotal_CapsDiscountAmount() {
        setupSecurityContext(adminUser);

        OrderItemRequest itemReq = OrderItemRequest.builder().productId(10L).quantity(1).build(); // Subtotal = 100.00
        CreateOrderRequest request = CreateOrderRequest.builder()
                .source(OrderSource.OFFLINE)
                .discountId(200L) // Flat 200.00
                .items(List.of(itemReq))
                .build();

        when(merchantRepository.findById(1L)).thenReturn(Optional.of(merchant));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(discountRepository.findByIdAndMerchantId(200L, 1L)).thenReturn(Optional.of(flatDiscount));
        when(orderRepository.getNextOrderNumberSequence()).thenReturn(1001L);

        Order savedOrder = Order.builder()
                .merchant(merchant)
                .orderNumber("ORD-20260624-0001")
                .status(OrderStatus.NEW)
                .source(OrderSource.OFFLINE)
                .subtotal(new BigDecimal("100.00"))
                .discountAmount(new BigDecimal("100.00")) // Capped to subtotal
                .finalAmount(BigDecimal.ZERO)
                .build();
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        when(orderMapper.toResponse(any(Order.class))).thenReturn(
                OrderResponse.builder()
                        .subtotal(new BigDecimal("100.00"))
                        .discountAmount(new BigDecimal("100.00"))
                        .finalAmount(BigDecimal.ZERO)
                        .build()
        );

        OrderResponse result = orderService.createOrder(request);

        assertNotNull(result);
        assertEquals(new BigDecimal("100.00"), result.getSubtotal());
        assertEquals(new BigDecimal("100.00"), result.getDiscountAmount());
        assertEquals(BigDecimal.ZERO, result.getFinalAmount());
    }

    @Test
    void testCreateOrder_InactiveDiscount_ThrowsInactiveDiscountException() {
        setupSecurityContext(adminUser);
        percentageDiscount.setActive(false);

        OrderItemRequest itemReq = OrderItemRequest.builder().productId(10L).quantity(2).build();
        CreateOrderRequest request = CreateOrderRequest.builder()
                .discountId(100L)
                .items(List.of(itemReq))
                .build();

        when(merchantRepository.findById(1L)).thenReturn(Optional.of(merchant));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(discountRepository.findByIdAndMerchantId(100L, 1L)).thenReturn(Optional.of(percentageDiscount));

        assertThrows(InactiveDiscountException.class, () -> orderService.createOrder(request));
    }

    @Test
    void testCreateOrder_CrossTenantDiscount_ThrowsDiscountNotFoundException() {
        setupSecurityContext(adminUser);

        OrderItemRequest itemReq = OrderItemRequest.builder().productId(10L).quantity(2).build();
        CreateOrderRequest request = CreateOrderRequest.builder()
                .discountId(999L)
                .items(List.of(itemReq))
                .build();

        when(merchantRepository.findById(1L)).thenReturn(Optional.of(merchant));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(discountRepository.findByIdAndMerchantId(999L, 1L)).thenReturn(Optional.empty());

        assertThrows(DiscountNotFoundException.class, () -> orderService.createOrder(request));
    }
}

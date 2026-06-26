package com.campuscafe.backend.order.service;

import com.campuscafe.backend.domain.merchant.Merchant;
import com.campuscafe.backend.domain.order.Order;
import com.campuscafe.backend.domain.order.OrderItem;
import com.campuscafe.backend.domain.order.enums.OrderPriority;
import com.campuscafe.backend.domain.order.enums.OrderSource;
import com.campuscafe.backend.domain.order.enums.OrderStatus;
import com.campuscafe.backend.domain.product.Product;
import com.campuscafe.backend.domain.product.ProductRecipe;
import com.campuscafe.backend.domain.inventory.InventoryItem;
import com.campuscafe.backend.domain.inventory.InventoryTransaction;
import com.campuscafe.backend.domain.inventory.enums.InventoryTransactionType;
import com.campuscafe.backend.exception.*;
import com.campuscafe.backend.order.dto.OrderBoardResponse;
import com.campuscafe.backend.order.dto.OrderResponse;
import com.campuscafe.backend.order.dto.UpdateOrderPriorityRequest;
import com.campuscafe.backend.order.dto.UpdateOrderStatusRequest;
import com.campuscafe.backend.order.mapper.OrderMapper;
import com.campuscafe.backend.order.repository.OrderRepository;
import com.campuscafe.backend.product.repository.ProductRecipeRepository;
import com.campuscafe.backend.inventory.repository.InventoryItemRepository;
import com.campuscafe.backend.inventory.repository.InventoryTransactionRepository;
import com.campuscafe.backend.repository.NotificationRepository;
import com.campuscafe.backend.repository.UserRepository;
import com.campuscafe.backend.security.service.CustomUserDetails;
import com.campuscafe.backend.domain.user.User;
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
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderPriorityAndDeductionServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRecipeRepository productRecipeRepository;

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @Mock
    private InventoryTransactionRepository inventoryTransactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderService orderService;

    private Merchant merchant;
    private User adminUser;
    private Order order;
    private Product product;
    private InventoryItem inventoryItem;
    private ProductRecipe recipe;

    @BeforeEach
    void setUp() {
        merchant = Merchant.builder().email("owner@campuscafe.com").verified(true).active(true).build();
        merchant.setId(1L);

        adminUser = User.builder().merchant(merchant).name("Admin").email("owner@campuscafe.com").active(true).build();
        adminUser.setId(1L);

        product = Product.builder().merchant(merchant).name("Latte").price(BigDecimal.TEN).available(true).build();
        product.setId(10L);

        inventoryItem = InventoryItem.builder().merchant(merchant).name("Milk").unit("LITER").currentStock(new BigDecimal("5.00")).minStock(BigDecimal.ONE).build();
        inventoryItem.setId(20L);

        recipe = ProductRecipe.builder().merchant(merchant).product(product).inventoryItem(inventoryItem).quantityRequired(new BigDecimal("0.50")).build();
        recipe.setId(100L);

        order = Order.builder()
                .merchant(merchant)
                .orderNumber("ORD-20260624-1001")
                .status(OrderStatus.READY)
                .priority(OrderPriority.MEDIUM)
                .source(OrderSource.OFFLINE)
                .createdBy(adminUser)
                .build();
        order.setId(200L);

        OrderItem orderItem = OrderItem.builder().order(order).product(product).quantity(2).unitPrice(BigDecimal.TEN).subtotal(new BigDecimal("20.00")).build();
        order.addItem(orderItem);
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
    void testUpdateOrderPriority_Success() {
        setupSecurityContext(adminUser);

        UpdateOrderPriorityRequest request = UpdateOrderPriorityRequest.builder()
                .priority("URGENT")
                .build();

        when(orderRepository.findById(200L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toResponse(any(Order.class))).thenReturn(new OrderResponse());

        OrderResponse result = orderService.updateOrderPriority(200L, request);

        assertNotNull(result);
        assertEquals(OrderPriority.URGENT, order.getPriority());
    }

    @Test
    void testUpdateOrderPriority_CompletedOrder_ThrowsException() {
        setupSecurityContext(adminUser);
        order.setStatus(OrderStatus.COMPLETED);

        UpdateOrderPriorityRequest request = UpdateOrderPriorityRequest.builder()
                .priority("HIGH")
                .build();

        when(orderRepository.findById(200L)).thenReturn(Optional.of(order));

        assertThrows(InvalidOrderStatusException.class, () -> orderService.updateOrderPriority(200L, request));
    }

    @Test
    void testGetOrderBoard_OptimizedActiveOrdersOnly() {
        setupSecurityContext(adminUser);

        when(orderRepository.findActiveOrdersSorted(eq(1L), anyList())).thenReturn(List.of(order));
        when(orderMapper.toResponse(any(Order.class))).thenReturn(OrderResponse.builder().status("READY").build());

        OrderBoardResponse board = orderService.getOrderBoard();

        assertNotNull(board);
        assertEquals(1, board.getReady().size());
        assertEquals(0, board.getPreparing().size());
        assertEquals(0, board.getNewOrders().size());
    }

    @Test
    void testUpdateOrderStatus_ToCompleted_TriggersDeduction() {
        setupSecurityContext(adminUser);

        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status("COMPLETED")
                .build();

        when(orderRepository.findById(200L)).thenReturn(Optional.of(order));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(productRecipeRepository.findByProductId(10L)).thenReturn(List.of(recipe));
        when(inventoryItemRepository.findById(20L)).thenReturn(Optional.of(inventoryItem));
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenReturn(inventoryItem);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toResponse(any(Order.class))).thenReturn(new OrderResponse());

        OrderResponse response = orderService.updateOrderStatus(200L, request);

        assertNotNull(response);
        assertEquals(OrderStatus.COMPLETED, order.getStatus());
        
        // Milk stock starts at 5.0, requires 2 * 0.50 = 1.0, so final is 4.0
        assertEquals(new BigDecimal("4.00"), inventoryItem.getCurrentStock());
        
        verify(inventoryTransactionRepository, times(1)).save(any(InventoryTransaction.class));
    }

    @Test
    void testUpdateOrderStatus_ToCompleted_InsufficientStock_RollsBack() {
        setupSecurityContext(adminUser);
        inventoryItem.setCurrentStock(new BigDecimal("0.80")); // Required is 1.0

        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status("COMPLETED")
                .build();

        when(orderRepository.findById(200L)).thenReturn(Optional.of(order));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(productRecipeRepository.findByProductId(10L)).thenReturn(List.of(recipe));
        when(inventoryItemRepository.findById(20L)).thenReturn(Optional.of(inventoryItem));

        assertThrows(InsufficientInventoryException.class, () -> orderService.updateOrderStatus(200L, request));
        
        // Ensure status was not updated and repositories did not save changes
        assertNotEquals(OrderStatus.COMPLETED, order.getStatus());
        verify(inventoryItemRepository, never()).save(any(InventoryItem.class));
        verify(inventoryTransactionRepository, never()).save(any(InventoryTransaction.class));
    }
}

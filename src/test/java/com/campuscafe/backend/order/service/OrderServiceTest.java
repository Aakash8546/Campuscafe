package com.campuscafe.backend.order.service;

import com.campuscafe.backend.domain.merchant.Merchant;
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
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

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
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderService orderService;

    private Merchant merchant;
    private Merchant otherMerchant;
    private Product product;
    private Product otherProduct;
    private User adminUser;

    @BeforeEach
    void setUp() {
        merchant = Merchant.builder().cafeName("Merchant A").email("merchantA@test.com").verified(true).build();
        merchant.setId(1L);

        otherMerchant = Merchant.builder().cafeName("Merchant B").email("merchantB@test.com").verified(true).build();
        otherMerchant.setId(2L);

        product = Product.builder().name("Coffee").price(new BigDecimal("4.50")).merchant(merchant).available(true).build();
        product.setId(10L);

        otherProduct = Product.builder().name("Sandwich").price(new BigDecimal("8.00")).merchant(otherMerchant).available(true).build();
        otherProduct.setId(20L);

        com.campuscafe.backend.domain.user.Role adminRole = com.campuscafe.backend.domain.user.Role.builder().name("ADMIN").build();
        adminUser = User.builder()
                .email("admin@merchantA.com")
                .role(adminRole)
                .merchant(merchant)
                .active(true)
                .build();
        adminUser.setId(1L);
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
    void testCreateOrder_Success() {
        setupSecurityContext(adminUser);

        OrderItemRequest itemReq = OrderItemRequest.builder().productId(10L).quantity(2).build();
        CreateOrderRequest request = CreateOrderRequest.builder()
                .source(OrderSource.OFFLINE)
                .items(List.of(itemReq))
                .build();

        when(merchantRepository.findById(1L)).thenReturn(Optional.of(merchant));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(orderRepository.getNextOrderNumberSequence()).thenReturn(1001L);

        Order savedOrder = Order.builder()
                .merchant(merchant)
                .orderNumber("ORD-20260624-0001")
                .status(OrderStatus.NEW)
                .source(OrderSource.OFFLINE)
                .subtotal(new BigDecimal("9.00"))
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(new BigDecimal("9.00"))
                .build();
        savedOrder.setId(100L);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        OrderResponse response = OrderResponse.builder()
                .id(100L)
                .orderNumber("ORD-20260624-0001")
                .status("NEW")
                .finalAmount(new BigDecimal("9.00"))
                .build();
        when(orderMapper.toResponse(any(Order.class))).thenReturn(response);

        OrderResponse result = orderService.createOrder(request);

        assertNotNull(result);
        assertEquals("ORD-20260624-0001", result.getOrderNumber());
        assertEquals("NEW", result.getStatus());
        assertEquals(new BigDecimal("9.00"), result.getFinalAmount());

        verify(orderRepository, times(1)).save(any(Order.class));
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void testCreateOrder_ProductUnavailable_ThrowsProductUnavailableException() {
        setupSecurityContext(adminUser);
        product.setAvailable(false);

        OrderItemRequest itemReq = OrderItemRequest.builder().productId(10L).quantity(2).build();
        CreateOrderRequest request = CreateOrderRequest.builder()
                .items(List.of(itemReq))
                .build();

        when(merchantRepository.findById(1L)).thenReturn(Optional.of(merchant));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        assertThrows(ProductUnavailableException.class, () -> orderService.createOrder(request));
    }

    @Test
    void testCreateOrder_CrossTenantProduct_ThrowsAccessDeniedException() {
        setupSecurityContext(adminUser);

        OrderItemRequest itemReq = OrderItemRequest.builder().productId(20L).quantity(2).build();
        CreateOrderRequest request = CreateOrderRequest.builder()
                .items(List.of(itemReq))
                .build();

        when(merchantRepository.findById(1L)).thenReturn(Optional.of(merchant));
        when(productRepository.findById(20L)).thenReturn(Optional.of(otherProduct));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        assertThrows(AccessDeniedException.class, () -> orderService.createOrder(request));
    }

    @Test
    void testUpdateOrderStatus_ValidTransition() {
        setupSecurityContext(adminUser);

        Order order = Order.builder()
                .merchant(merchant)
                .status(OrderStatus.NEW)
                .build();
        order.setId(100L);

        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder().status("PREPARING").build();

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        OrderResponse response = OrderResponse.builder().id(100L).status("PREPARING").build();
        when(orderMapper.toResponse(any(Order.class))).thenReturn(response);

        OrderResponse result = orderService.updateOrderStatus(100L, request);

        assertNotNull(result);
        assertEquals("PREPARING", result.getStatus());
    }

    @Test
    void testUpdateOrderStatus_InvalidTransition_ThrowsInvalidOrderTransitionException() {
        setupSecurityContext(adminUser);

        Order order = Order.builder()
                .merchant(merchant)
                .status(OrderStatus.COMPLETED)
                .build();
        order.setId(100L);

        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder().status("NEW").build();

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        assertThrows(InvalidOrderTransitionException.class, () -> orderService.updateOrderStatus(100L, request));
    }

    @Test
    void testGetDashboardMetrics() {
        setupSecurityContext(adminUser);

        when(orderRepository.countByMerchantIdAndCreatedAtBetween(eq(1L), any(Instant.class), any(Instant.class))).thenReturn(25L);
        when(orderRepository.sumRevenueByMerchantIdAndStatusAndCreatedAtBetween(eq(1L), eq(OrderStatus.COMPLETED), any(Instant.class), any(Instant.class)))
                .thenReturn(new BigDecimal("500.00"));
        when(orderRepository.countByMerchantIdAndStatus(1L, OrderStatus.NEW)).thenReturn(5L);
        when(orderRepository.countByMerchantIdAndStatus(1L, OrderStatus.READY)).thenReturn(2L);

        OrderDashboardResponse result = orderService.getDashboardMetrics();

        assertNotNull(result);
        assertEquals(25L, result.getTodayOrders());
        assertEquals(new BigDecimal("500.00"), result.getTodayRevenue());
        assertEquals(5L, result.getNewOrders());
        assertEquals(2L, result.getReadyOrders());
    }
}

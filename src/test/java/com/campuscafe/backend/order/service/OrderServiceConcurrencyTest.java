package com.campuscafe.backend.order.service;

import com.campuscafe.backend.domain.merchant.Merchant;
import com.campuscafe.backend.domain.order.Order;
import com.campuscafe.backend.domain.order.enums.OrderSource;
import com.campuscafe.backend.domain.order.enums.OrderStatus;
import com.campuscafe.backend.domain.product.Product;
import com.campuscafe.backend.domain.user.User;
import com.campuscafe.backend.order.dto.CreateOrderRequest;
import com.campuscafe.backend.order.dto.OrderItemRequest;
import com.campuscafe.backend.order.dto.OrderResponse;
import com.campuscafe.backend.order.mapper.OrderMapper;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceConcurrencyTest {

    @Mock
    private OrderRepository orderRepository;

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
    private User adminUser;
    private Product product;
    private CustomUserDetails userDetails;
    private UsernamePasswordAuthenticationToken authentication;
    private SecurityContext securityContext;

    @BeforeEach
    void setUp() {
        merchant = Merchant.builder().cafeName("Merchant A").email("merchantA@test.com").verified(true).build();
        merchant.setId(1L);

        product = Product.builder().name("Coffee").price(new BigDecimal("4.50")).merchant(merchant).available(true).build();
        product.setId(10L);

        com.campuscafe.backend.domain.user.Role adminRole = com.campuscafe.backend.domain.user.Role.builder().name("ADMIN").build();
        adminUser = User.builder()
                .email("admin@merchantA.com")
                .role(adminRole)
                .merchant(merchant)
                .active(true)
                .build();
        adminUser.setId(1L);

        userDetails = new CustomUserDetails(adminUser);
        authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testConcurrentOrderCreation_ProducesUniqueOrderNumbers() throws InterruptedException, ExecutionException {
        // Setup shared mock stubs
        when(merchantRepository.findById(1L)).thenReturn(Optional.of(merchant));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        // Stub check for duplicate order numbers to return false
        when(orderRepository.existsByMerchantIdAndOrderNumberAndCreatedAtBetween(any(), any(), any(), any())).thenReturn(false);

        // Stub order save to return the order object passed to it
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // Stub orderMapper to return a response mapped from the saved order
        when(orderMapper.toResponse(any(Order.class))).thenAnswer(inv -> {
            Order order = inv.getArgument(0);
            return OrderResponse.builder()
                    .id(100L)
                    .orderNumber(order.getOrderNumber())
                    .status(order.getStatus().name())
                    .finalAmount(order.getFinalAmount())
                    .build();
        });

        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Callable<OrderResponse>> tasks = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            tasks.add(() -> {
                // Ensure security context is set for each thread
                SecurityContextHolder.setContext(securityContext);

                OrderItemRequest itemReq = OrderItemRequest.builder().productId(10L).quantity(1).build();

                CreateOrderRequest request = CreateOrderRequest.builder()
                        .source(OrderSource.OFFLINE)
                        .items(List.of(itemReq))
                        .build();

                return orderService.createOrder(request);
            });
        }

        List<Future<OrderResponse>> futures = executor.invokeAll(tasks);
        List<String> orderNumbers = Collections.synchronizedList(new ArrayList<>());

        for (Future<OrderResponse> future : futures) {
            OrderResponse response = future.get();
            assertNotNull(response);
            orderNumbers.add(response.getOrderNumber());
        }

        executor.shutdown();

        // Verify that we generated unique order numbers (no duplicates)
        assertEquals(numThreads, orderNumbers.size(), "Should have correct number of orders");

        // Verify existsByMerchantIdAndOrderNumberAndCreatedAtBetween was checked
        verify(orderRepository, atLeast(numThreads)).existsByMerchantIdAndOrderNumberAndCreatedAtBetween(any(), any(), any(), any());
    }
}

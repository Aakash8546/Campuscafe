package com.campuscafe.backend.dashboard.service;

import com.campuscafe.backend.dashboard.dto.*;
import com.campuscafe.backend.dashboard.repository.DashboardInventoryItemRepository;
import com.campuscafe.backend.dashboard.repository.DashboardOrderRepository;
import com.campuscafe.backend.dashboard.repository.DashboardProductRepository;
import com.campuscafe.backend.domain.inventory.InventoryItem;
import com.campuscafe.backend.domain.merchant.Merchant;
import com.campuscafe.backend.domain.order.Order;
import com.campuscafe.backend.domain.order.enums.OrderStatus;
import com.campuscafe.backend.domain.user.Role;
import com.campuscafe.backend.domain.user.User;
import com.campuscafe.backend.exception.AccessDeniedException;
import com.campuscafe.backend.exception.InvalidDateRangeException;
import com.campuscafe.backend.security.service.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private DashboardOrderRepository dashboardOrderRepository;

    @Mock
    private DashboardProductRepository dashboardProductRepository;

    @Mock
    private DashboardInventoryItemRepository dashboardInventoryItemRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private Merchant merchant;
    private User adminUser;

    @BeforeEach
    void setUp() {
        merchant = Merchant.builder().cafeName("Cafe A").email("cafeA@test.com").verified(true).build();
        merchant.setId(1L);

        Role adminRole = Role.builder().name("ADMIN").build();
        adminUser = User.builder().email("admin@cafeA.com").role(adminRole).merchant(merchant).active(true).build();
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
    void testGetSummary_Success() {
        setupSecurityContext(adminUser);

        when(dashboardOrderRepository.countByMerchantIdAndCreatedAtBetween(eq(1L), any(Instant.class), any(Instant.class))).thenReturn(45L);
        when(dashboardOrderRepository.sumRevenueByMerchantIdAndStatusAndCreatedAtBetween(eq(1L), eq(OrderStatus.COMPLETED), any(Instant.class), any(Instant.class)))
                .thenReturn(new BigDecimal("15200.00"));
        when(dashboardOrderRepository.countByMerchantIdAndStatusAndCreatedAtBetween(eq(1L), eq(OrderStatus.COMPLETED), any(Instant.class), any(Instant.class)))
                .thenReturn(35L);
        when(dashboardOrderRepository.countByMerchantIdAndStatusAndCreatedAtBetween(eq(1L), eq(OrderStatus.CANCELLED), any(Instant.class), any(Instant.class)))
                .thenReturn(3L);
        when(dashboardInventoryItemRepository.countLowStockItemsByMerchantId(1L)).thenReturn(10L);
        when(dashboardProductRepository.countByMerchantId(1L)).thenReturn(120L);

        DashboardSummaryResponse result = dashboardService.getSummary();

        assertNotNull(result);
        assertEquals(45L, result.getTodayOrders());
        assertEquals(new BigDecimal("15200.00"), result.getTodayRevenue());
        assertEquals(35L, result.getCompletedOrders());
        assertEquals(3L, result.getCancelledOrders());
        assertEquals(10L, result.getLowStockItems());
        assertEquals(120L, result.getTotalProducts());
    }

    @Test
    void testGetRecentOrders_Success() {
        setupSecurityContext(adminUser);

        Order order = Order.builder()
                .orderNumber("ORD-1")
                .status(OrderStatus.NEW)
                .finalAmount(new BigDecimal("50.00"))
                .build();
        order.setCreatedAt(Instant.now());

        when(dashboardOrderRepository.findTop10ByMerchantIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(order));

        List<RecentOrderResponse> result = dashboardService.getRecentOrders();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ORD-1", result.get(0).getOrderNumber());
    }

    @Test
    void testGetSalesOverview_Success() {
        setupSecurityContext(adminUser);

        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 6, 2);

        Object[] row = new Object[]{"2026-06-01", new BigDecimal("12000.00"), 45L};
        when(dashboardOrderRepository.getSalesOverview(eq(1L), eq(OrderStatus.COMPLETED), any(Instant.class), any(Instant.class)))
                .thenReturn(List.<Object[]>of(row));

        List<SalesOverviewResponse> result = dashboardService.getSalesOverview(from, to);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("2026-06-01", result.get(0).getDate());
        assertEquals(new BigDecimal("12000.00"), result.get(0).getRevenue());
        assertEquals(45L, result.get(0).getOrders());
    }

    @Test
    void testGetSalesOverview_InvalidRange_ThrowsInvalidDateRangeException() {
        LocalDate from = LocalDate.of(2026, 6, 2);
        LocalDate to = LocalDate.of(2026, 6, 1);

        assertThrows(InvalidDateRangeException.class, () -> dashboardService.getSalesOverview(from, to));
    }

    @Test
    void testGetTopProducts_Success() {
        setupSecurityContext(adminUser);

        Object[] row = new Object[]{10L, "Cold Coffee", 250L, new BigDecimal("12500.00")};
        when(dashboardOrderRepository.getTopSellingProducts(eq(1L), eq(OrderStatus.COMPLETED), eq(PageRequest.of(0, 5))))
                .thenReturn(List.<Object[]>of(row));

        List<TopProductResponse> result = dashboardService.getTopProducts(5);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getProductId());
        assertEquals("Cold Coffee", result.get(0).getProductName());
        assertEquals(250L, result.get(0).getQuantitySold());
        assertEquals(new BigDecimal("12500.00"), result.get(0).getRevenue());
    }

    @Test
    void testGetOrderStatusDistribution_Success() {
        setupSecurityContext(adminUser);

        Object[] newRow = new Object[]{OrderStatus.NEW, 10L};
        Object[] completedRow = new Object[]{OrderStatus.COMPLETED, 150L};

        when(dashboardOrderRepository.getOrderStatusDistribution(1L)).thenReturn(List.<Object[]>of(newRow, completedRow));

        OrderStatusDistributionResponse result = dashboardService.getOrderStatusDistribution();

        assertNotNull(result);
        assertEquals(10L, result.getNewOrders());
        assertEquals(150L, result.getCompleted());
        assertEquals(0L, result.getPreparing());
    }

    @Test
    void testGetLowStockItems_Success() {
        setupSecurityContext(adminUser);

        InventoryItem item = InventoryItem.builder()
                .name("Milk")
                .currentStock(new BigDecimal("5.000"))
                .minStock(new BigDecimal("10.000"))
                .unit("LITER")
                .build();
        item.setId(200L);

        when(dashboardInventoryItemRepository.findLowStockItemsByMerchantId(1L)).thenReturn(List.of(item));

        List<LowStockItemResponse> result = dashboardService.getLowStockItems();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Milk", result.get(0).getName());
        assertEquals(new BigDecimal("5.000"), result.get(0).getCurrentStock());
        assertEquals("LITER", result.get(0).getUnit());
    }
}

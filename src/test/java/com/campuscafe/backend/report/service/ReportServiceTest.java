package com.campuscafe.backend.report.service;

import com.campuscafe.backend.domain.merchant.Merchant;
import com.campuscafe.backend.domain.order.Order;
import com.campuscafe.backend.domain.order.enums.OrderSource;
import com.campuscafe.backend.domain.order.enums.OrderStatus;
import com.campuscafe.backend.domain.user.Role;
import com.campuscafe.backend.domain.user.User;
import com.campuscafe.backend.exception.AccessDeniedException;
import com.campuscafe.backend.exception.InvalidDateRangeException;
import com.campuscafe.backend.exception.ReportGenerationException;
import com.campuscafe.backend.order.dto.OrderResponse;
import com.campuscafe.backend.order.mapper.OrderMapper;
import com.campuscafe.backend.report.dto.*;
import com.campuscafe.backend.report.repository.ReportInventoryItemRepository;
import com.campuscafe.backend.report.repository.ReportOrderItemRepository;
import com.campuscafe.backend.report.repository.ReportOrderRepository;
import com.campuscafe.backend.security.service.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportOrderRepository reportOrderRepository;

    @Mock
    private ReportOrderItemRepository reportOrderItemRepository;

    @Mock
    private ReportInventoryItemRepository reportInventoryItemRepository;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private ReportService reportService;

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
    void testGetDailySalesReport_Success() {
        setupSecurityContext(adminUser);

        LocalDate date = LocalDate.of(2026, 6, 24);

        when(reportOrderRepository.countByMerchantIdAndStatusAndCreatedAtBetween(eq(1L), eq(OrderStatus.COMPLETED), any(Instant.class), any(Instant.class)))
                .thenReturn(45L);
        when(reportOrderRepository.sumRevenueByMerchantIdAndStatusAndCreatedAtBetween(eq(1L), eq(OrderStatus.COMPLETED), any(Instant.class), any(Instant.class)))
                .thenReturn(new BigDecimal("15200.00"));

        DailySalesReportResponse result = reportService.getDailySalesReport(date);

        assertNotNull(result);
        assertEquals("2026-06-24", result.getDate());
        assertEquals(45L, result.getOrders());
        assertEquals(new BigDecimal("15200.00"), result.getRevenue());
        assertEquals(new BigDecimal("337.78"), result.getAverageOrderValue()); // 15200 / 45 = 337.7777...
    }

    @Test
    void testGetMonthlySalesReport_Success() {
        setupSecurityContext(adminUser);

        when(reportOrderRepository.countByMerchantIdAndStatusAndCreatedAtBetween(eq(1L), eq(OrderStatus.COMPLETED), any(Instant.class), any(Instant.class)))
                .thenReturn(1200L);
        when(reportOrderRepository.sumRevenueByMerchantIdAndStatusAndCreatedAtBetween(eq(1L), eq(OrderStatus.COMPLETED), any(Instant.class), any(Instant.class)))
                .thenReturn(new BigDecimal("520000.00"));

        MonthlySalesReportResponse result = reportService.getMonthlySalesReport(2026, 6);

        assertNotNull(result);
        assertEquals("JUNE", result.getMonth());
        assertEquals(1200L, result.getOrders());
        assertEquals(new BigDecimal("520000.00"), result.getRevenue());
        assertEquals(new BigDecimal("433.33"), result.getAverageOrderValue()); // 520000 / 1200 = 433.3333...
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetOrdersReport_Success() {
        setupSecurityContext(adminUser);

        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 6, 2);

        Order order = Order.builder().orderNumber("ORD-1").build();
        Page<Order> page = new PageImpl<>(List.of(order));

        when(reportOrderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(orderMapper.toResponse(any(Order.class))).thenReturn(OrderResponse.builder().orderNumber("ORD-1").build());

        Page<OrderResponse> result = reportService.getOrdersReport(OrderStatus.NEW, OrderSource.OFFLINE, from, to, PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("ORD-1", result.getContent().get(0).getOrderNumber());
    }

    @Test
    void testGetRevenueReport_Success() {
        setupSecurityContext(adminUser);

        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 6, 2);

        Object[] row = new Object[]{new BigDecimal("100000.00"), new BigDecimal("5000.00"), new BigDecimal("95000.00")};
        when(reportOrderRepository.getRevenueReportMetrics(eq(1L), eq(OrderStatus.COMPLETED), any(Instant.class), any(Instant.class)))
                .thenReturn(List.<Object[]>of(row));

        RevenueReportResponse result = reportService.getRevenueReport(from, to);

        assertNotNull(result);
        assertEquals(new BigDecimal("100000.00"), result.getGrossRevenue());
        assertEquals(new BigDecimal("5000.00"), result.getDiscountAmount());
        assertEquals(new BigDecimal("95000.00"), result.getNetRevenue());
    }

    @Test
    void testGetInventoryReport_Success() {
        setupSecurityContext(adminUser);

        when(reportInventoryItemRepository.countByMerchantId(1L)).thenReturn(150L);
        when(reportInventoryItemRepository.countLowStockItemsByMerchantId(1L)).thenReturn(10L);
        when(reportInventoryItemRepository.countOutOfStockItemsByMerchantId(1L)).thenReturn(2L);

        InventoryReportResponse result = reportService.getInventoryReport();

        assertNotNull(result);
        assertEquals(150L, result.getTotalItems());
        assertEquals(10L, result.getLowStockItems());
        assertEquals(2L, result.getOutOfStockItems());
    }

    @Test
    void testGetProductPerformanceReport_Success() {
        setupSecurityContext(adminUser);

        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 6, 2);

        Object[] row = new Object[]{"Burger", 100L, new BigDecimal("25000.00")};
        when(reportOrderItemRepository.getProductPerformanceReport(eq(1L), eq(OrderStatus.COMPLETED), any(Instant.class), any(Instant.class)))
                .thenReturn(List.<Object[]>of(row));

        List<ProductPerformanceResponse> result = reportService.getProductPerformanceReport(from, to);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Burger", result.get(0).getProductName());
        assertEquals(100L, result.get(0).getQuantitySold());
        assertEquals(new BigDecimal("25000.00"), result.get(0).getRevenue());
    }
}

package com.campuscafe.backend.integration;

import com.campuscafe.backend.dashboard.controller.DashboardController;
import com.campuscafe.backend.dashboard.dto.DashboardSummaryResponse;
import com.campuscafe.backend.dashboard.dto.OrderStatusDistributionResponse;
import com.campuscafe.backend.dashboard.service.DashboardService;
import com.campuscafe.backend.domain.merchant.Merchant;
import com.campuscafe.backend.domain.order.enums.OrderSource;
import com.campuscafe.backend.domain.order.enums.OrderStatus;
import com.campuscafe.backend.domain.user.Permission;
import com.campuscafe.backend.domain.user.Role;
import com.campuscafe.backend.domain.user.User;
import com.campuscafe.backend.order.dto.OrderResponse;
import com.campuscafe.backend.report.controller.ReportController;
import com.campuscafe.backend.report.dto.DailySalesReportResponse;
import com.campuscafe.backend.report.dto.InventoryReportResponse;
import com.campuscafe.backend.report.dto.RevenueReportResponse;
import com.campuscafe.backend.report.service.ReportService;
import com.campuscafe.backend.security.config.SecurityConfig;
import com.campuscafe.backend.security.filter.JwtAuthenticationFilter;
import com.campuscafe.backend.security.service.CustomUserDetails;
import com.campuscafe.backend.security.service.CustomUserDetailsService;
import com.campuscafe.backend.security.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({DashboardController.class, ReportController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class Phase8ControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private ReportService reportService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    private CustomUserDetails adminPrincipal;
    private CustomUserDetails managerPrincipal;
    private CustomUserDetails cashierPrincipal;
    private CustomUserDetails guestPrincipal;

    @BeforeEach
    void setUp() {
        Merchant merchant = Merchant.builder().email("merchant@test.com").build();
        merchant.setId(1L);

        Set<Permission> allPermissions = Set.of(
                "DASHBOARD_VIEW", "REPORT_VIEW"
        ).stream().map(name -> Permission.builder().name(name).build()).collect(Collectors.toSet());

        Set<Permission> cashierPermissions = Set.of(
                "DASHBOARD_VIEW"
        ).stream().map(name -> Permission.builder().name(name).build()).collect(Collectors.toSet());

        Role adminRole = Role.builder().name("ADMIN").permissions(allPermissions).build();
        User admin = User.builder().email("admin@test.com").role(adminRole).merchant(merchant).active(true).build();
        admin.setId(1L);
        adminPrincipal = new CustomUserDetails(admin);

        Role managerRole = Role.builder().name("MANAGER").permissions(allPermissions).build();
        User manager = User.builder().email("manager@test.com").role(managerRole).merchant(merchant).active(true).build();
        manager.setId(2L);
        managerPrincipal = new CustomUserDetails(manager);

        Role cashierRole = Role.builder().name("CASHIER").permissions(cashierPermissions).build();
        User cashier = User.builder().email("cashier@test.com").role(cashierRole).merchant(merchant).active(true).build();
        cashier.setId(3L);
        cashierPrincipal = new CustomUserDetails(cashier);

        Role guestRole = Role.builder().name("GUEST").permissions(Collections.emptySet()).build();
        User guest = User.builder().email("guest@test.com").role(guestRole).merchant(merchant).active(true).build();
        guest.setId(4L);
        guestPrincipal = new CustomUserDetails(guest);
    }

    // ----------------------------------------------------
    // DASHBOARD ENDPOINT TESTS
    // ----------------------------------------------------

    @Test
    void testGetSummary_AsCashier_Success() throws Exception {
        DashboardSummaryResponse response = DashboardSummaryResponse.builder()
                .todayOrders(45)
                .todayRevenue(new BigDecimal("15200.00"))
                .completedOrders(35)
                .cancelledOrders(3)
                .lowStockItems(10)
                .totalProducts(120)
                .build();

        when(dashboardService.getSummary()).thenReturn(response);

        mockMvc.perform(get("/dashboard/summary")
                        .with(user(cashierPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.todayOrders").value(45))
                .andExpect(jsonPath("$.data.todayRevenue").value(15200.00));
    }

    @Test
    void testGetSummary_AsGuest_Forbidden() throws Exception {
        mockMvc.perform(get("/dashboard/summary")
                        .with(user(guestPrincipal)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetOrderStatusDistribution_AsCashier_Success() throws Exception {
        OrderStatusDistributionResponse response = OrderStatusDistributionResponse.builder()
                .newOrders(10)
                .preparing(5)
                .ready(3)
                .completed(150)
                .cancelled(7)
                .build();

        when(dashboardService.getOrderStatusDistribution()).thenReturn(response);

        mockMvc.perform(get("/dashboard/order-status-distribution")
                        .with(user(cashierPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.new").value(10))
                .andExpect(jsonPath("$.data.completed").value(150));
    }

    // ----------------------------------------------------
    // REPORT ENDPOINT TESTS
    // ----------------------------------------------------

    @Test
    void testGetDailySales_AsManager_Success() throws Exception {
        DailySalesReportResponse response = DailySalesReportResponse.builder()
                .date("2026-06-24")
                .orders(45)
                .revenue(new BigDecimal("15200.00"))
                .averageOrderValue(new BigDecimal("337.78"))
                .build();

        when(reportService.getDailySalesReport(eq(LocalDate.of(2026, 6, 24)))).thenReturn(response);

        mockMvc.perform(get("/reports/daily-sales")
                        .with(user(managerPrincipal))
                        .param("date", "2026-06-24"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orders").value(45))
                .andExpect(jsonPath("$.data.averageOrderValue").value(337.78));
    }

    @Test
    void testGetDailySales_AsCashier_Forbidden() throws Exception {
        mockMvc.perform(get("/reports/daily-sales")
                        .with(user(cashierPrincipal))
                        .param("date", "2026-06-24"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetOrdersReport_AsManager_Success() throws Exception {
        OrderResponse orderRes = OrderResponse.builder().orderNumber("ORD-123").build();
        Page<OrderResponse> page = new PageImpl<>(List.of(orderRes));

        when(reportService.getOrdersReport(eq(OrderStatus.COMPLETED), eq(null), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/reports/orders")
                        .with(user(managerPrincipal))
                        .param("status", "COMPLETED")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].orderNumber").value("ORD-123"));
    }

    @Test
    void testGetRevenueReport_AsManager_Success() throws Exception {
        RevenueReportResponse response = RevenueReportResponse.builder()
                .grossRevenue(new BigDecimal("100000.00"))
                .discountAmount(new BigDecimal("5000.00"))
                .netRevenue(new BigDecimal("95000.00"))
                .build();

        when(reportService.getRevenueReport(eq(LocalDate.of(2026, 6, 1)), eq(LocalDate.of(2026, 6, 2))))
                .thenReturn(response);

        mockMvc.perform(get("/reports/revenue")
                        .with(user(managerPrincipal))
                        .param("fromDate", "2026-06-01")
                        .param("toDate", "2026-06-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.grossRevenue").value(100000.00))
                .andExpect(jsonPath("$.data.netRevenue").value(95000.00));
    }

    @Test
    void testGetInventoryReport_AsManager_Success() throws Exception {
        InventoryReportResponse response = InventoryReportResponse.builder()
                .totalItems(150)
                .lowStockItems(10)
                .outOfStockItems(2)
                .build();

        when(reportService.getInventoryReport()).thenReturn(response);

        mockMvc.perform(get("/reports/inventory")
                        .with(user(managerPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalItems").value(150))
                .andExpect(jsonPath("$.data.outOfStockItems").value(2));
    }
}

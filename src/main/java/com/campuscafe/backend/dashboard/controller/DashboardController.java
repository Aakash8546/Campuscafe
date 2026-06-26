package com.campuscafe.backend.dashboard.controller;

import com.campuscafe.backend.common.ApiResponse;
import com.campuscafe.backend.dashboard.dto.*;
import com.campuscafe.backend.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Validated
@Tag(name = "Dashboard & Analytics", description = "Endpoints for retrieving merchant sales statistics and inventory alert summaries")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    @Operation(summary = "Get today's dashboard summary metrics", description = "Requires DASHBOARD_VIEW.")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary() {
        DashboardSummaryResponse response = dashboardService.getSummary();
        return ResponseEntity.ok(ApiResponse.success("Dashboard summary retrieved successfully", response));
    }

    @GetMapping("/recent-orders")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    @Operation(summary = "Get the latest 10 orders", description = "Requires DASHBOARD_VIEW.")
    public ResponseEntity<ApiResponse<List<RecentOrderResponse>>> getRecentOrders() {
        List<RecentOrderResponse> response = dashboardService.getRecentOrders();
        return ResponseEntity.ok(ApiResponse.success("Recent orders retrieved successfully", response));
    }

    @GetMapping("/sales-overview")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    @Operation(summary = "Get sales overview data grouped by date", description = "Requires DASHBOARD_VIEW.")
    public ResponseEntity<ApiResponse<List<SalesOverviewResponse>>> getSalesOverview(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        List<SalesOverviewResponse> response = dashboardService.getSalesOverview(fromDate, toDate);
        return ResponseEntity.ok(ApiResponse.success("Sales overview retrieved successfully", response));
    }

    @GetMapping("/top-products")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    @Operation(summary = "Get top selling products list", description = "Requires DASHBOARD_VIEW.")
    public ResponseEntity<ApiResponse<List<TopProductResponse>>> getTopProducts(
            @RequestParam(defaultValue = "5") int limit
    ) {
        List<TopProductResponse> response = dashboardService.getTopProducts(limit);
        return ResponseEntity.ok(ApiResponse.success("Top selling products retrieved successfully", response));
    }

    @GetMapping("/order-status-distribution")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    @Operation(summary = "Get status distribution counts for merchant orders", description = "Requires DASHBOARD_VIEW.")
    public ResponseEntity<ApiResponse<OrderStatusDistributionResponse>> getOrderStatusDistribution() {
        OrderStatusDistributionResponse response = dashboardService.getOrderStatusDistribution();
        return ResponseEntity.ok(ApiResponse.success("Order status distribution retrieved successfully", response));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    @Operation(summary = "Get list of items that are low in stock", description = "Requires DASHBOARD_VIEW.")
    public ResponseEntity<ApiResponse<List<LowStockItemResponse>>> getLowStock() {
        List<LowStockItemResponse> response = dashboardService.getLowStockItems();
        return ResponseEntity.ok(ApiResponse.success("Low stock items retrieved successfully", response));
    }
}

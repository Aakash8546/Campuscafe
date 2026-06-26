package com.campuscafe.backend.report.controller;

import com.campuscafe.backend.common.ApiResponse;
import com.campuscafe.backend.domain.order.enums.OrderSource;
import com.campuscafe.backend.domain.order.enums.OrderStatus;
import com.campuscafe.backend.order.dto.OrderResponse;
import com.campuscafe.backend.report.dto.*;
import com.campuscafe.backend.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
@RequestMapping("/reports")
@RequiredArgsConstructor
@Validated
@Tag(name = "Reporting & Analytics", description = "Endpoints for generating detailed sales, order, revenue, and inventory reports")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/daily-sales")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    @Operation(summary = "Get daily sales report metrics", description = "Requires REPORT_VIEW.")
    public ResponseEntity<ApiResponse<DailySalesReportResponse>> getDailySalesReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        DailySalesReportResponse response = reportService.getDailySalesReport(date);
        return ResponseEntity.ok(ApiResponse.success("Daily sales report retrieved successfully", response));
    }

    @GetMapping("/monthly-sales")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    @Operation(summary = "Get monthly sales report metrics", description = "Requires REPORT_VIEW.")
    public ResponseEntity<ApiResponse<MonthlySalesReportResponse>> getMonthlySalesReport(
            @RequestParam int year,
            @RequestParam int month
    ) {
        MonthlySalesReportResponse response = reportService.getMonthlySalesReport(year, month);
        return ResponseEntity.ok(ApiResponse.success("Monthly sales report retrieved successfully", response));
    }

    @GetMapping("/orders")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    @Operation(summary = "Get paginated order reports with dynamic filtering", description = "Requires REPORT_VIEW.")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getOrdersReport(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) OrderSource source,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @ParameterObject Pageable pageable
    ) {
        Page<OrderResponse> response = reportService.getOrdersReport(status, source, fromDate, toDate, pageable);
        return ResponseEntity.ok(ApiResponse.success("Orders report retrieved successfully", response));
    }

    @GetMapping("/revenue")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    @Operation(summary = "Get gross vs net revenue report for a date range", description = "Requires REPORT_VIEW.")
    public ResponseEntity<ApiResponse<RevenueReportResponse>> getRevenueReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        RevenueReportResponse response = reportService.getRevenueReport(fromDate, toDate);
        return ResponseEntity.ok(ApiResponse.success("Revenue report retrieved successfully", response));
    }

    @GetMapping("/inventory")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    @Operation(summary = "Get inventory stock level summary report", description = "Requires REPORT_VIEW.")
    public ResponseEntity<ApiResponse<InventoryReportResponse>> getInventoryReport() {
        InventoryReportResponse response = reportService.getInventoryReport();
        return ResponseEntity.ok(ApiResponse.success("Inventory report retrieved successfully", response));
    }

    @GetMapping("/products")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    @Operation(summary = "Get product sales performance statistics for a date range", description = "Requires REPORT_VIEW.")
    public ResponseEntity<ApiResponse<List<ProductPerformanceResponse>>> getProductPerformanceReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        List<ProductPerformanceResponse> response = reportService.getProductPerformanceReport(fromDate, toDate);
        return ResponseEntity.ok(ApiResponse.success("Product performance report retrieved successfully", response));
    }

    @GetMapping("/sales/daily/products")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    @Operation(summary = "Get daily sales grouped by product", description = "Requires REPORT_VIEW.")
    public ResponseEntity<ApiResponse<List<ProductPerformanceResponse>>> getDailyProductSales(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<ProductPerformanceResponse> response = reportService.getProductPerformanceReport(date, date);
        return ResponseEntity.ok(ApiResponse.success("Daily product sales retrieved successfully", response));
    }

    @GetMapping("/sales/daily/categories")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    @Operation(summary = "Get daily sales grouped by category", description = "Requires REPORT_VIEW.")
    public ResponseEntity<ApiResponse<List<CategoryPerformanceResponse>>> getDailyCategorySales(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<CategoryPerformanceResponse> response = reportService.getCategoryPerformanceReport(date, date);
        return ResponseEntity.ok(ApiResponse.success("Daily category sales retrieved successfully", response));
    }

    @GetMapping("/sales/monthly/products")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    @Operation(summary = "Get monthly sales grouped by product", description = "Requires REPORT_VIEW.")
    public ResponseEntity<ApiResponse<List<ProductPerformanceResponse>>> getMonthlyProductSales(
            @RequestParam int year,
            @RequestParam int month
    ) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);
        List<ProductPerformanceResponse> response = reportService.getProductPerformanceReport(start, end);
        return ResponseEntity.ok(ApiResponse.success("Monthly product sales retrieved successfully", response));
    }

    @GetMapping("/sales/monthly/categories")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    @Operation(summary = "Get monthly sales grouped by category", description = "Requires REPORT_VIEW.")
    public ResponseEntity<ApiResponse<List<CategoryPerformanceResponse>>> getMonthlyCategorySales(
            @RequestParam int year,
            @RequestParam int month
    ) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);
        List<CategoryPerformanceResponse> response = reportService.getCategoryPerformanceReport(start, end);
        return ResponseEntity.ok(ApiResponse.success("Monthly category sales retrieved successfully", response));
    }
}

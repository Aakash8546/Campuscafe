package com.campuscafe.backend.report.service;

import com.campuscafe.backend.domain.order.Order;
import com.campuscafe.backend.domain.order.enums.OrderSource;
import com.campuscafe.backend.domain.order.enums.OrderStatus;
import com.campuscafe.backend.exception.AccessDeniedException;
import com.campuscafe.backend.exception.InvalidDateRangeException;
import com.campuscafe.backend.exception.ReportGenerationException;
import com.campuscafe.backend.order.dto.OrderResponse;
import com.campuscafe.backend.order.mapper.OrderMapper;
import com.campuscafe.backend.order.specification.OrderSpecification;
import com.campuscafe.backend.report.dto.*;
import com.campuscafe.backend.report.repository.ReportInventoryItemRepository;
import com.campuscafe.backend.report.repository.ReportOrderItemRepository;
import com.campuscafe.backend.report.repository.ReportOrderRepository;
import com.campuscafe.backend.security.service.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportOrderRepository reportOrderRepository;
    private final ReportOrderItemRepository reportOrderItemRepository;
    private final ReportInventoryItemRepository reportInventoryItemRepository;
    private final OrderMapper orderMapper;

    private CustomUserDetails getAuthenticatedUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            throw new AccessDeniedException("User is not authenticated");
        }
        return (CustomUserDetails) authentication.getPrincipal();
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && toDate.isBefore(fromDate)) {
            throw new InvalidDateRangeException("toDate cannot be before fromDate");
        }
    }

    private BigDecimal calculateAverageOrderValue(BigDecimal revenue, long orders) {
        if (orders <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return revenue.divide(BigDecimal.valueOf(orders), 2, RoundingMode.HALF_UP);
    }

    public DailySalesReportResponse getDailySalesReport(LocalDate date) {
        if (date == null) {
            throw new ReportGenerationException("Date is required for daily sales report");
        }

        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        Instant start = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        long orders = reportOrderRepository.countByMerchantIdAndStatusAndCreatedAtBetween(merchantId, OrderStatus.COMPLETED, start, end);
        BigDecimal revenue = reportOrderRepository.sumRevenueByMerchantIdAndStatusAndCreatedAtBetween(merchantId, OrderStatus.COMPLETED, start, end);
        if (revenue == null) {
            revenue = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal averageOrderValue = calculateAverageOrderValue(revenue, orders);

        return DailySalesReportResponse.builder()
                .date(date.toString())
                .orders(orders)
                .revenue(revenue)
                .averageOrderValue(averageOrderValue)
                .build();
    }

    public MonthlySalesReportResponse getMonthlySalesReport(int year, int month) {
        if (month < 1 || month > 12) {
            throw new ReportGenerationException("Invalid month: " + month);
        }

        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        LocalDate startOfMonth = LocalDate.of(year, month, 1);
        LocalDate endOfMonth = startOfMonth.plusMonths(1);
        Instant start = startOfMonth.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = endOfMonth.atStartOfDay(ZoneOffset.UTC).toInstant();

        long orders = reportOrderRepository.countByMerchantIdAndStatusAndCreatedAtBetween(merchantId, OrderStatus.COMPLETED, start, end);
        BigDecimal revenue = reportOrderRepository.sumRevenueByMerchantIdAndStatusAndCreatedAtBetween(merchantId, OrderStatus.COMPLETED, start, end);
        if (revenue == null) {
            revenue = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal averageOrderValue = calculateAverageOrderValue(revenue, orders);

        return MonthlySalesReportResponse.builder()
                .month(Month.of(month).name())
                .orders(orders)
                .revenue(revenue)
                .averageOrderValue(averageOrderValue)
                .build();
    }

    public Page<OrderResponse> getOrdersReport(OrderStatus status, OrderSource source, LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        validateDateRange(fromDate, toDate);

        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        Specification<Order> spec = Specification.where(OrderSpecification.withMerchantId(merchantId));

        if (status != null) {
            spec = spec.and(OrderSpecification.withStatus(status));
        }
        if (source != null) {
            spec = spec.and(OrderSpecification.withSource(source));
        }
        if (fromDate != null || toDate != null) {
            Instant start = fromDate != null ? fromDate.atStartOfDay(ZoneOffset.UTC).toInstant() : null;
            Instant end = toDate != null ? toDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() : null;
            spec = spec.and(OrderSpecification.withDateRange(start, end));
        }

        Page<Order> orders = reportOrderRepository.findAll(spec, pageable);
        return orders.map(orderMapper::toResponse);
    }

    public RevenueReportResponse getRevenueReport(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            throw new InvalidDateRangeException("fromDate and toDate are required");
        }
        validateDateRange(fromDate, toDate);

        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        Instant start = fromDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = toDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<Object[]> rows = reportOrderRepository.getRevenueReportMetrics(merchantId, OrderStatus.COMPLETED, start, end);

        BigDecimal grossRevenue = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal discountAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal netRevenue = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        if (rows != null && !rows.isEmpty() && rows.get(0)[0] != null) {
            Object[] row = rows.get(0);
            grossRevenue = (BigDecimal) row[0];
            discountAmount = (BigDecimal) row[1];
            netRevenue = (BigDecimal) row[2];
        }

        return RevenueReportResponse.builder()
                .grossRevenue(grossRevenue)
                .discountAmount(discountAmount)
                .netRevenue(netRevenue)
                .build();
    }

    public InventoryReportResponse getInventoryReport() {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        long totalItems = reportInventoryItemRepository.countByMerchantId(merchantId);
        long lowStockItems = reportInventoryItemRepository.countLowStockItemsByMerchantId(merchantId);
        long outOfStockItems = reportInventoryItemRepository.countOutOfStockItemsByMerchantId(merchantId);

        return InventoryReportResponse.builder()
                .totalItems(totalItems)
                .lowStockItems(lowStockItems)
                .outOfStockItems(outOfStockItems)
                .build();
    }

    public List<ProductPerformanceResponse> getProductPerformanceReport(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            throw new InvalidDateRangeException("fromDate and toDate are required");
        }
        validateDateRange(fromDate, toDate);

        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        Instant start = fromDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = toDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<Object[]> rows = reportOrderItemRepository.getProductPerformanceReport(merchantId, OrderStatus.COMPLETED, start, end);
        List<ProductPerformanceResponse> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(ProductPerformanceResponse.builder()
                    .productName(row[0].toString())
                    .quantitySold(((Number) row[1]).longValue())
                    .revenue((BigDecimal) row[2])
                    .build());
        }
        return result;
    }

    public List<CategoryPerformanceResponse> getCategoryPerformanceReport(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            throw new InvalidDateRangeException("fromDate and toDate are required");
        }
        validateDateRange(fromDate, toDate);

        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        Instant start = fromDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = toDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<Object[]> rows = reportOrderItemRepository.getCategoryPerformanceReport(merchantId, OrderStatus.COMPLETED, start, end);
        List<CategoryPerformanceResponse> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(CategoryPerformanceResponse.builder()
                    .categoryName(row[0].toString())
                    .quantitySold(((Number) row[1]).longValue())
                    .revenue((BigDecimal) row[2])
                    .build());
        }
        return result;
    }
}

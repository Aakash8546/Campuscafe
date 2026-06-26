package com.campuscafe.backend.dashboard.service;

import com.campuscafe.backend.dashboard.dto.*;
import com.campuscafe.backend.dashboard.repository.DashboardInventoryItemRepository;
import com.campuscafe.backend.dashboard.repository.DashboardOrderRepository;
import com.campuscafe.backend.dashboard.repository.DashboardProductRepository;
import com.campuscafe.backend.domain.inventory.InventoryItem;
import com.campuscafe.backend.domain.order.Order;
import com.campuscafe.backend.domain.order.enums.OrderStatus;
import com.campuscafe.backend.exception.AccessDeniedException;
import com.campuscafe.backend.exception.InvalidDateRangeException;
import com.campuscafe.backend.security.service.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final DashboardOrderRepository dashboardOrderRepository;
    private final DashboardProductRepository dashboardProductRepository;
    private final DashboardInventoryItemRepository dashboardInventoryItemRepository;

    private CustomUserDetails getAuthenticatedUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            throw new AccessDeniedException("User is not authenticated");
        }
        return (CustomUserDetails) authentication.getPrincipal();
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            throw new InvalidDateRangeException("fromDate and toDate are required");
        }
        if (toDate.isBefore(fromDate)) {
            throw new InvalidDateRangeException("toDate cannot be before fromDate");
        }
    }

    public DashboardSummaryResponse getSummary() {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        LocalDate today = LocalDate.now();
        Instant start = today.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        long todayOrders = dashboardOrderRepository.countByMerchantIdAndCreatedAtBetween(merchantId, start, end);
        BigDecimal todayRevenue = dashboardOrderRepository.sumRevenueByMerchantIdAndStatusAndCreatedAtBetween(merchantId, OrderStatus.COMPLETED, start, end);
        long completedOrders = dashboardOrderRepository.countByMerchantIdAndStatusAndCreatedAtBetween(merchantId, OrderStatus.COMPLETED, start, end);
        long cancelledOrders = dashboardOrderRepository.countByMerchantIdAndStatusAndCreatedAtBetween(merchantId, OrderStatus.CANCELLED, start, end);
        long lowStockItems = dashboardInventoryItemRepository.countLowStockItemsByMerchantId(merchantId);
        long totalProducts = dashboardProductRepository.countByMerchantId(merchantId);

        return DashboardSummaryResponse.builder()
                .todayOrders(todayOrders)
                .todayRevenue(todayRevenue)
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .lowStockItems(lowStockItems)
                .totalProducts(totalProducts)
                .build();
    }

    public List<RecentOrderResponse> getRecentOrders() {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        List<Order> orders = dashboardOrderRepository.findTop10ByMerchantIdOrderByCreatedAtDesc(merchantId);
        return orders.stream()
                .map(o -> RecentOrderResponse.builder()
                        .orderNumber(o.getOrderNumber())
                        .status(o.getStatus().name())
                        .amount(o.getFinalAmount())
                        .createdAt(o.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    public List<SalesOverviewResponse> getSalesOverview(LocalDate fromDate, LocalDate toDate) {
        validateDateRange(fromDate, toDate);

        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        Instant start = fromDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = toDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<Object[]> rows = dashboardOrderRepository.getSalesOverview(merchantId, OrderStatus.COMPLETED, start, end);
        List<SalesOverviewResponse> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(SalesOverviewResponse.builder()
                    .date(row[0].toString())
                    .revenue((BigDecimal) row[1])
                    .orders(((Number) row[2]).longValue())
                    .build());
        }
        return result;
    }

    public List<TopProductResponse> getTopProducts(int limit) {
        if (limit <= 0) {
            limit = 5;
        }

        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        List<Object[]> rows = dashboardOrderRepository.getTopSellingProducts(merchantId, OrderStatus.COMPLETED, PageRequest.of(0, limit));
        List<TopProductResponse> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(TopProductResponse.builder()
                    .productId(((Number) row[0]).longValue())
                    .productName(row[1].toString())
                    .quantitySold(((Number) row[2]).longValue())
                    .revenue((BigDecimal) row[3])
                    .build());
        }
        return result;
    }

    public OrderStatusDistributionResponse getOrderStatusDistribution() {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        List<Object[]> rows = dashboardOrderRepository.getOrderStatusDistribution(merchantId);
        Map<OrderStatus, Long> distribution = new HashMap<>();
        for (Object[] row : rows) {
            distribution.put((OrderStatus) row[0], ((Number) row[1]).longValue());
        }

        return OrderStatusDistributionResponse.builder()
                .newOrders(distribution.getOrDefault(OrderStatus.NEW, 0L))
                .preparing(distribution.getOrDefault(OrderStatus.PREPARING, 0L))
                .ready(distribution.getOrDefault(OrderStatus.READY, 0L))
                .completed(distribution.getOrDefault(OrderStatus.COMPLETED, 0L))
                .cancelled(distribution.getOrDefault(OrderStatus.CANCELLED, 0L))
                .build();
    }

    public List<LowStockItemResponse> getLowStockItems() {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        List<InventoryItem> items = dashboardInventoryItemRepository.findLowStockItemsByMerchantId(merchantId);
        return items.stream()
                .map(i -> LowStockItemResponse.builder()
                        .id(i.getId())
                        .name(i.getName())
                        .currentStock(i.getCurrentStock())
                        .minStock(i.getMinStock())
                        .unit(i.getUnit())
                        .build())
                .collect(Collectors.toList());
    }
}

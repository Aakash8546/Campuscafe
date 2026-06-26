package com.campuscafe.backend.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {
    private long todayOrders;
    private BigDecimal todayRevenue;
    private long completedOrders;
    private long cancelledOrders;
    private long lowStockItems;
    private long totalProducts;
}

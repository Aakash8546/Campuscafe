package com.campuscafe.backend.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryDashboardResponse {
    private long totalItems;
    private long lowStockItems;
    private long totalCategories;
}

package com.campuscafe.backend.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItemResponse {
    private Long id;
    private String name;
    private String unit;
    private BigDecimal currentStock;
    private BigDecimal minStock;
    private BigDecimal maxStock;
    private InventoryCategoryResponse category;
    private Instant createdAt;
    private Instant updatedAt;
}

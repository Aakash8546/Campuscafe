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
public class LowStockItemResponse {
    private Long id;
    private String name;
    private BigDecimal currentStock;
    private BigDecimal minStock;
    private String unit;
}

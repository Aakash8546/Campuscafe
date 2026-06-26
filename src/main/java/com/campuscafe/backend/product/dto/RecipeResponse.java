package com.campuscafe.backend.product.dto;

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
public class RecipeResponse {
    private Long id;
    private Long merchantId;
    private Long productId;
    private Long inventoryItemId;
    private String inventoryItemName;
    private String unit;
    private BigDecimal quantityRequired;
    private Instant createdAt;
}

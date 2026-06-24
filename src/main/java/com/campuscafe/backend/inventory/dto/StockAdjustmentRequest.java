package com.campuscafe.backend.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAdjustmentRequest {

    @NotNull(message = "Inventory Item ID is required")
    private Long inventoryItemId;

    @NotNull(message = "New quantity is required")
    @DecimalMin(value = "0.000", inclusive = true, message = "New quantity must be greater than or equal to zero")
    private BigDecimal newQuantity;

    private String remarks;
}

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
public class StockOutRequest {

    @NotNull(message = "Inventory Item ID is required")
    private Long inventoryItemId;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.000", inclusive = false, message = "Quantity must be greater than zero")
    private BigDecimal quantity;

    private String remarks;
}

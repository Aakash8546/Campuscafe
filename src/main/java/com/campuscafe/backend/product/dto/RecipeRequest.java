package com.campuscafe.backend.product.dto;

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
public class RecipeRequest {

    @NotNull(message = "Inventory Item ID is required")
    private Long inventoryItemId;

    @NotNull(message = "Quantity required is required")
    @DecimalMin(value = "0.001", message = "Quantity required must be greater than zero")
    private BigDecimal quantityRequired;
}

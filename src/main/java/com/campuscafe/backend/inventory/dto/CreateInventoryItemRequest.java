package com.campuscafe.backend.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInventoryItemRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @NotBlank(message = "Unit is required")
    @Pattern(regexp = "^(GRAM|KG|ML|LITER|PIECE|PACKET|BOX)$", 
             message = "Unit must be one of: GRAM, KG, ML, LITER, PIECE, PACKET, BOX")
    private String unit;

    @NotNull(message = "Current stock is required")
    @DecimalMin(value = "0.000", message = "Current stock must be greater than or equal to 0")
    private BigDecimal currentStock;

    @NotNull(message = "Min stock is required")
    @DecimalMin(value = "0.000", message = "Min stock must be greater than or equal to 0")
    private BigDecimal minStock;

    @NotNull(message = "Max stock is required")
    @DecimalMin(value = "0.000", message = "Max stock must be greater than or equal to 0")
    private BigDecimal maxStock;

    @NotNull(message = "Category ID is required")
    private Long categoryId;
}

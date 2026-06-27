package com.campuscafe.backend.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class ProductVariantRequest {

    private Long id;

    @NotBlank(message = "Variant name is required")
    @Size(max = 50, message = "Variant name must not exceed 50 characters")
    private String name;

    @NotNull(message = "Variant price is required")
    @DecimalMin(value = "0.00", inclusive = false, message = "Variant price must be greater than zero")
    private BigDecimal price;

    private Boolean available;
}

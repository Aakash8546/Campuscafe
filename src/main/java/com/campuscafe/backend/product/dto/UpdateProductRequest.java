package com.campuscafe.backend.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(max = 100, message = "Product name must not exceed 100 characters")
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.00", inclusive = false, message = "Price must be greater than zero")
    private BigDecimal price;

    @Size(max = 255, message = "Image URL must not exceed 255 characters")
    private String imageUrl;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @Min(value = 0, message = "Priority must be greater than or equal to zero")
    private Integer priority;

    @Valid
    private List<ProductVariantRequest> variants;
}

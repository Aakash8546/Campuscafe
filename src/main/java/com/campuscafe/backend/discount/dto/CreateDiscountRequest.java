package com.campuscafe.backend.discount.dto;

import com.campuscafe.backend.domain.discount.enums.DiscountType;
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
public class CreateDiscountRequest {

    @NotBlank(message = "Discount name is required")
    @Size(max = 100, message = "Discount name must not exceed 100 characters")
    private String name;

    @NotNull(message = "Discount type is required")
    private DiscountType discountType;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.01", message = "Discount value must be greater than zero")
    private BigDecimal value;

    @DecimalMin(value = "0.01", message = "Max discount must be greater than zero")
    private BigDecimal maxDiscount;

    @Builder.Default
    private Boolean active = true;
}

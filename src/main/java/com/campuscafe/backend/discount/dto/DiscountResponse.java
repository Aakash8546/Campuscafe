package com.campuscafe.backend.discount.dto;

import com.campuscafe.backend.domain.discount.enums.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountResponse {
    private Long id;
    private String name;
    private DiscountType discountType;
    private BigDecimal value;
    private BigDecimal maxDiscount;
    private Boolean active;
}

package com.campuscafe.backend.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSummaryResponse {
    private Long id;
    private String name;
    private BigDecimal price;
    private String imageUrl;
    private Boolean available;
    private String categoryName;
}

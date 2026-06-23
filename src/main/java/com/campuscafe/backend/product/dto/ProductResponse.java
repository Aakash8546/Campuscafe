package com.campuscafe.backend.product.dto;

import com.campuscafe.backend.category.dto.CategoryResponse;
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
public class ProductResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private Boolean available;
    private CategoryResponse category;
    private Instant createdAt;
    private Instant updatedAt;
}

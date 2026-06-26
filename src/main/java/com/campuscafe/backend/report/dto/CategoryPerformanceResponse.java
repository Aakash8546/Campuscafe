package com.campuscafe.backend.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryPerformanceResponse {
    private String categoryName;
    private Long quantitySold;
    private BigDecimal revenue;
}

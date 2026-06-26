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
public class DailySalesReportResponse {
    private String date;
    private long orders;
    private BigDecimal revenue;
    private BigDecimal averageOrderValue;
}

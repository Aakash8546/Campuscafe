package com.campuscafe.backend.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailsResponse {
    private Long id;
    private String orderNumber;
    private String status;
    private String priority;
    private String source;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String createdByEmail;
    private String createdByName;
    private List<OrderItemResponse> items;
    private Instant createdAt;
    private Instant updatedAt;
}

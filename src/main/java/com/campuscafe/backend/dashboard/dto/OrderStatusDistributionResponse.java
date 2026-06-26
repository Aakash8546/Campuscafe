package com.campuscafe.backend.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusDistributionResponse {
    @JsonProperty("new")
    private long newOrders;
    private long preparing;
    private long ready;
    private long completed;
    private long cancelled;
}

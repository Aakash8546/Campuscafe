package com.campuscafe.backend.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderBoardResponse {

    @JsonProperty("new")
    private List<OrderResponse> newOrders;

    private List<OrderResponse> preparing;

    private List<OrderResponse> ready;
}

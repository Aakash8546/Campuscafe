package com.campuscafe.backend.customer.controller;

import com.campuscafe.backend.common.ApiResponse;
import com.campuscafe.backend.order.dto.OrderResponse;
import com.campuscafe.backend.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders/public")
@RequiredArgsConstructor
@Tag(name = "Customer Display Monitor", description = "Public endpoints for counter status displays")
public class CustomerDisplayController {

    private final OrderService orderService;

    @GetMapping("/display")
    @Operation(summary = "Get active customer order statuses for counter display screen")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getPublicCustomerDisplayOrders(@RequestParam(required = false, defaultValue = "1") Long merchantId) {
        List<OrderResponse> response = orderService.getPublicCustomerDisplayOrders(merchantId);
        return ResponseEntity.ok(ApiResponse.success("Customer display orders retrieved successfully", response));
    }
}

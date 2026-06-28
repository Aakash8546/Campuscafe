package com.campuscafe.backend.kot.controller;

import com.campuscafe.backend.common.ApiResponse;
import com.campuscafe.backend.order.dto.OrderDetailsResponse;
import com.campuscafe.backend.order.dto.OrderResponse;
import com.campuscafe.backend.order.dto.UpdateOrderStatusRequest;
import com.campuscafe.backend.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/kot")
@RequiredArgsConstructor
@Validated
@Tag(name = "Kitchen Order Ticket (KOT) Management", description = "Endpoints for kitchen staff and managers to track and update live kitchen tickets")
@SecurityRequirement(name = "bearerAuth")
public class KotController {

    private final OrderService orderService;

    @GetMapping("/active")
    @PreAuthorize("hasAnyAuthority('KOT_VIEW', 'ORDER_VIEW')")
    @Operation(summary = "Get active KOT tickets", description = "Accessible by ADMIN, MANAGER, and KITCHEN_STAFF.")
    public ResponseEntity<ApiResponse<List<OrderDetailsResponse>>> getActiveKotOrders() {
        List<OrderDetailsResponse> response = orderService.getActiveKotOrders();
        return ResponseEntity.ok(ApiResponse.success("Active KOT tickets retrieved successfully", response));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('KOT_UPDATE', 'ORDER_UPDATE')")
    @Operation(summary = "Update KOT ticket status", description = "Supported flow: PENDING -> PREPARING -> READY -> SERVED. Accessible by ADMIN, MANAGER, and KITCHEN_STAFF.")
    public ResponseEntity<ApiResponse<OrderResponse>> updateKotStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        OrderResponse response = orderService.updateOrderStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("KOT status updated successfully", response));
    }
}

package com.campuscafe.backend.order.controller;

import com.campuscafe.backend.common.ApiResponse;
import com.campuscafe.backend.domain.order.enums.OrderSource;
import com.campuscafe.backend.domain.order.enums.OrderStatus;
import com.campuscafe.backend.order.dto.*;
import com.campuscafe.backend.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Validated
@Tag(name = "Order & Billing Management", description = "Endpoints for order checkout, billing workflow, boards, and dashboards")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasAuthority('ORDER_CREATE')")
    @Operation(summary = "Checkout and create a new order", description = "Direct billing screen call. Requires ORDER_CREATE.")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request
    ) {
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.ok(ApiResponse.success("Order created successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ORDER_VIEW')")
    @Operation(summary = "Get filtered and paginated order listings", description = "Requires ORDER_VIEW.")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) OrderSource source,
            @RequestParam(required = false) String orderNumber,
            @RequestParam(required = false) Instant startDate,
            @RequestParam(required = false) Instant endDate,
            @ParameterObject Pageable pageable
    ) {
        Page<OrderResponse> response = orderService.getOrders(status, source, orderNumber, startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully", response));
    }

    @GetMapping("/board")
    @PreAuthorize("hasAuthority('ORDER_VIEW')")
    @Operation(summary = "Get active orders structured for Kanban board", description = "Requires ORDER_VIEW.")
    public ResponseEntity<ApiResponse<OrderBoardResponse>> getOrderBoard() {
        OrderBoardResponse response = orderService.getOrderBoard();
        return ResponseEntity.ok(ApiResponse.success("Order board retrieved successfully", response));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('ORDER_VIEW')")
    @Operation(summary = "Get dashboard metrics for today's sales and queue backlogs", description = "Requires ORDER_VIEW.")
    public ResponseEntity<ApiResponse<OrderDashboardResponse>> getDashboardMetrics() {
        OrderDashboardResponse response = orderService.getDashboardMetrics();
        return ResponseEntity.ok(ApiResponse.success("Dashboard metrics retrieved successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ORDER_VIEW')")
    @Operation(summary = "Get complete order and order item details by ID", description = "Requires ORDER_VIEW.")
    public ResponseEntity<ApiResponse<OrderDetailsResponse>> getOrderById(@PathVariable Long id) {
        OrderDetailsResponse response = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success("Order details retrieved successfully", response));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ORDER_UPDATE')")
    @Operation(summary = "Update order status (NEW -> PREPARING -> READY -> COMPLETED)", description = "Enforces valid state machine flows. Requires ORDER_UPDATE.")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        OrderResponse response = orderService.updateOrderStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Order status updated successfully", response));
    }

    @PatchMapping("/{id}/priority")
    @PreAuthorize("hasAuthority('ORDER_UPDATE')")
    @Operation(summary = "Update order priority", description = "Only active orders can change priority. Completed and Cancelled cannot. Requires ORDER_UPDATE.")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderPriority(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderPriorityRequest request
    ) {
        OrderResponse response = orderService.updateOrderPriority(id, request);
        return ResponseEntity.ok(ApiResponse.success("Order priority updated successfully", response));
    }
}

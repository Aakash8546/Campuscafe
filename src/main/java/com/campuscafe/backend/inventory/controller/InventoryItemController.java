package com.campuscafe.backend.inventory.controller;

import com.campuscafe.backend.common.ApiResponse;
import com.campuscafe.backend.inventory.dto.*;
import com.campuscafe.backend.inventory.service.InventoryItemService;
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

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
@Validated
@Tag(name = "Inventory Item & Stock Management", description = "Endpoints for inventory items and stock movements")
@SecurityRequirement(name = "bearerAuth")
public class InventoryItemController {

    private final InventoryItemService inventoryItemService;

    @PostMapping("/items")
    @PreAuthorize("hasAuthority('INVENTORY_CREATE')")
    @Operation(summary = "Create a new inventory item", description = "Accessible by ADMIN and MANAGER.")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> createItem(
            @Valid @RequestBody CreateInventoryItemRequest request
    ) {
        InventoryItemResponse response = inventoryItemService.createItem(request);
        return ResponseEntity.ok(ApiResponse.success("Inventory item created successfully", response));
    }

    @GetMapping("/items")
    @PreAuthorize("hasAuthority('INVENTORY_VIEW')")
    @Operation(summary = "Get filtered and paginated inventory items", description = "Accessible by ADMIN, MANAGER, and CASHIER.")
    public ResponseEntity<ApiResponse<Page<InventoryItemSummaryResponse>>> getAllItems(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean lowStock,
            @ParameterObject Pageable pageable
    ) {
        Page<InventoryItemSummaryResponse> response = inventoryItemService.getAllItems(name, categoryId, lowStock, pageable);
        return ResponseEntity.ok(ApiResponse.success("Inventory items retrieved successfully", response));
    }

    @GetMapping("/items/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_VIEW')")
    @Operation(summary = "Get detailed inventory item by ID", description = "Accessible by ADMIN, MANAGER, and CASHIER.")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> getItemById(@PathVariable Long id) {
        InventoryItemResponse response = inventoryItemService.getItemById(id);
        return ResponseEntity.ok(ApiResponse.success("Inventory item retrieved successfully", response));
    }

    @PutMapping("/items/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_UPDATE')")
    @Operation(summary = "Update inventory item details", description = "Accessible by ADMIN and MANAGER.")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> updateItem(
            @PathVariable Long id,
            @Valid @RequestBody UpdateInventoryItemRequest request
    ) {
        InventoryItemResponse response = inventoryItemService.updateItem(id, request);
        return ResponseEntity.ok(ApiResponse.success("Inventory item updated successfully", response));
    }

    @DeleteMapping("/items/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_DELETE')")
    @Operation(summary = "Delete inventory item by ID", description = "Accessible by ADMIN only.")
    public ResponseEntity<ApiResponse<Void>> deleteItem(@PathVariable Long id) {
        inventoryItemService.deleteItem(id);
        return ResponseEntity.ok(ApiResponse.success("Inventory item deleted successfully", null));
    }

    @PostMapping("/stock-in")
    @PreAuthorize("hasAuthority('INVENTORY_UPDATE')")
    @Operation(summary = "Perform a stock in operation", description = "Increases item stock. Accessible by ADMIN and MANAGER.")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> stockIn(
            @Valid @RequestBody StockInRequest request
    ) {
        InventoryItemResponse response = inventoryItemService.stockIn(request);
        return ResponseEntity.ok(ApiResponse.success("Stock added successfully", response));
    }

    @PostMapping("/stock-out")
    @PreAuthorize("hasAuthority('INVENTORY_UPDATE')")
    @Operation(summary = "Perform a stock out operation", description = "Decreases item stock. Accessible by ADMIN and MANAGER.")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> stockOut(
            @Valid @RequestBody StockOutRequest request
    ) {
        InventoryItemResponse response = inventoryItemService.stockOut(request);
        return ResponseEntity.ok(ApiResponse.success("Stock removed successfully", response));
    }

    @PostMapping("/adjustment")
    @PreAuthorize("hasAuthority('INVENTORY_UPDATE')")
    @Operation(summary = "Adjust item stock to a specific physical quantity", description = "Accessible by ADMIN and MANAGER.")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> adjustStock(
            @Valid @RequestBody StockAdjustmentRequest request
    ) {
        InventoryItemResponse response = inventoryItemService.adjustStock(request);
        return ResponseEntity.ok(ApiResponse.success("Stock adjusted successfully", response));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('INVENTORY_VIEW')")
    @Operation(summary = "Get high-level summary of inventory metrics", description = "Accessible by ADMIN, MANAGER, and CASHIER.")
    public ResponseEntity<ApiResponse<InventoryDashboardResponse>> getDashboardMetrics() {
        InventoryDashboardResponse response = inventoryItemService.getDashboardMetrics();
        return ResponseEntity.ok(ApiResponse.success("Dashboard metrics retrieved successfully", response));
    }
}

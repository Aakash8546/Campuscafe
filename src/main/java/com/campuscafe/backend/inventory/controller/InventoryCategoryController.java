package com.campuscafe.backend.inventory.controller;

import com.campuscafe.backend.common.ApiResponse;
import com.campuscafe.backend.inventory.dto.InventoryCategoryResponse;
import com.campuscafe.backend.inventory.dto.CreateInventoryCategoryRequest;
import com.campuscafe.backend.inventory.dto.UpdateInventoryCategoryRequest;
import com.campuscafe.backend.inventory.service.InventoryCategoryService;
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
@RequestMapping("/inventory/categories")
@RequiredArgsConstructor
@Validated
@Tag(name = "Inventory Category Management", description = "Endpoints for managing inventory categories with tenant isolation")
@SecurityRequirement(name = "bearerAuth")
public class InventoryCategoryController {

    private final InventoryCategoryService inventoryCategoryService;

    @PostMapping
    @PreAuthorize("hasAuthority('INVENTORY_CREATE')")
    @Operation(summary = "Create a new inventory category", description = "Accessible by ADMIN and MANAGER.")
    public ResponseEntity<ApiResponse<InventoryCategoryResponse>> createCategory(
            @Valid @RequestBody CreateInventoryCategoryRequest request
    ) {
        InventoryCategoryResponse response = inventoryCategoryService.createCategory(request);
        return ResponseEntity.ok(ApiResponse.success("Inventory category created successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('INVENTORY_VIEW')")
    @Operation(summary = "Get all inventory categories of the authenticated merchant", description = "Accessible by ADMIN, MANAGER, and CASHIER.")
    public ResponseEntity<ApiResponse<List<InventoryCategoryResponse>>> getAllCategories() {
        List<InventoryCategoryResponse> response = inventoryCategoryService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success("Inventory categories retrieved successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_VIEW')")
    @Operation(summary = "Get detailed inventory category by ID", description = "Accessible by ADMIN, MANAGER, and CASHIER.")
    public ResponseEntity<ApiResponse<InventoryCategoryResponse>> getCategoryById(@PathVariable Long id) {
        InventoryCategoryResponse response = inventoryCategoryService.getCategoryById(id);
        return ResponseEntity.ok(ApiResponse.success("Inventory category retrieved successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_UPDATE')")
    @Operation(summary = "Update inventory category details", description = "Accessible by ADMIN and MANAGER.")
    public ResponseEntity<ApiResponse<InventoryCategoryResponse>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody UpdateInventoryCategoryRequest request
    ) {
        InventoryCategoryResponse response = inventoryCategoryService.updateCategory(id, request);
        return ResponseEntity.ok(ApiResponse.success("Inventory category updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_DELETE')")
    @Operation(summary = "Delete inventory category by ID", description = "Accessible by ADMIN only.")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        inventoryCategoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Inventory category deleted successfully", null));
    }
}

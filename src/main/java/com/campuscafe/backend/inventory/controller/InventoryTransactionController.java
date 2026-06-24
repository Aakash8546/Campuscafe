package com.campuscafe.backend.inventory.controller;

import com.campuscafe.backend.common.ApiResponse;
import com.campuscafe.backend.inventory.dto.InventoryTransactionResponse;
import com.campuscafe.backend.inventory.service.InventoryItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inventory/transactions")
@RequiredArgsConstructor
@Tag(name = "Inventory Transaction Logs", description = "Endpoints for checking transaction logs and inventory flow history")
@SecurityRequirement(name = "bearerAuth")
public class InventoryTransactionController {

    private final InventoryItemService inventoryItemService;

    @GetMapping
    @PreAuthorize("hasAuthority('INVENTORY_VIEW')")
    @Operation(summary = "Get paginated history of inventory transactions", description = "Accessible by ADMIN, MANAGER, and CASHIER.")
    public ResponseEntity<ApiResponse<Page<InventoryTransactionResponse>>> getTransactionHistory(
            @ParameterObject Pageable pageable
    ) {
        Page<InventoryTransactionResponse> response = inventoryItemService.getTransactionHistory(pageable);
        return ResponseEntity.ok(ApiResponse.success("Inventory transactions retrieved successfully", response));
    }
}

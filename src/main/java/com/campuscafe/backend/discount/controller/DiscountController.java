package com.campuscafe.backend.discount.controller;

import com.campuscafe.backend.common.ApiResponse;
import com.campuscafe.backend.discount.dto.CreateDiscountRequest;
import com.campuscafe.backend.discount.dto.DiscountResponse;
import com.campuscafe.backend.discount.dto.UpdateDiscountRequest;
import com.campuscafe.backend.discount.service.DiscountService;
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
@RequestMapping("/discounts")
@RequiredArgsConstructor
@Validated
@Tag(name = "Discount Management", description = "Endpoints for managing merchant-level discounts and deactivation/activation")
@SecurityRequirement(name = "bearerAuth")
public class DiscountController {

    private final DiscountService discountService;

    @PostMapping
    @PreAuthorize("hasAuthority('DISCOUNT_CREATE')")
    @Operation(summary = "Create a new discount", description = "Requires DISCOUNT_CREATE.")
    public ResponseEntity<ApiResponse<DiscountResponse>> createDiscount(
            @Valid @RequestBody CreateDiscountRequest request
    ) {
        DiscountResponse response = discountService.createDiscount(request);
        return ResponseEntity.ok(ApiResponse.success("Discount created successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('DISCOUNT_VIEW')")
    @Operation(summary = "Get all discounts for the merchant", description = "Requires DISCOUNT_VIEW.")
    public ResponseEntity<ApiResponse<List<DiscountResponse>>> getDiscounts() {
        List<DiscountResponse> response = discountService.getDiscounts();
        return ResponseEntity.ok(ApiResponse.success("Discounts retrieved successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('DISCOUNT_VIEW')")
    @Operation(summary = "Get discount by ID", description = "Requires DISCOUNT_VIEW.")
    public ResponseEntity<ApiResponse<DiscountResponse>> getDiscountById(
            @PathVariable Long id
    ) {
        DiscountResponse response = discountService.getDiscountById(id);
        return ResponseEntity.ok(ApiResponse.success("Discount retrieved successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('DISCOUNT_UPDATE')")
    @Operation(summary = "Update discount details", description = "Requires DISCOUNT_UPDATE.")
    public ResponseEntity<ApiResponse<DiscountResponse>> updateDiscount(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDiscountRequest request
    ) {
        DiscountResponse response = discountService.updateDiscount(id, request);
        return ResponseEntity.ok(ApiResponse.success("Discount updated successfully", response));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('DISCOUNT_UPDATE')")
    @Operation(summary = "Activate a discount", description = "Requires DISCOUNT_UPDATE.")
    public ResponseEntity<ApiResponse<DiscountResponse>> activateDiscount(
            @PathVariable Long id
    ) {
        DiscountResponse response = discountService.activateDiscount(id);
        return ResponseEntity.ok(ApiResponse.success("Discount activated successfully", response));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('DISCOUNT_UPDATE')")
    @Operation(summary = "Deactivate a discount", description = "Requires DISCOUNT_UPDATE.")
    public ResponseEntity<ApiResponse<DiscountResponse>> deactivateDiscount(
            @PathVariable Long id
    ) {
        DiscountResponse response = discountService.deactivateDiscount(id);
        return ResponseEntity.ok(ApiResponse.success("Discount deactivated successfully", response));
    }
}

package com.campuscafe.backend.product.controller;

import com.campuscafe.backend.common.ApiResponse;
import com.campuscafe.backend.product.dto.*;
import com.campuscafe.backend.product.service.ProductService;
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
@RequestMapping("/products")
@RequiredArgsConstructor
@Validated
@Tag(name = "Product Management", description = "Endpoints for managing products with search, pagination, and sorting")
@SecurityRequirement(name = "bearerAuth")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @PreAuthorize("hasAuthority('PRODUCT_CREATE')")
    @Operation(summary = "Create a new product", description = "Accessible by ADMIN and MANAGER.")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.ok(ApiResponse.success("Product created successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PRODUCT_VIEW')")
    @Operation(summary = "Search products with pagination and sorting", description = "Accessible by ADMIN, MANAGER, and CASHIER. Cashiers only see available products.")
    public ResponseEntity<ApiResponse<Page<ProductSummaryResponse>>> searchProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean available,
            @ParameterObject Pageable pageable
    ) {
        Page<ProductSummaryResponse> response = productService.searchProducts(name, categoryId, available, pageable);
        return ResponseEntity.ok(ApiResponse.success("Products retrieved successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUCT_VIEW')")
    @Operation(summary = "Get detailed product by ID", description = "Accessible by ADMIN, MANAGER, and CASHIER.")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        ProductResponse response = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success("Product retrieved successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUCT_UPDATE')")
    @Operation(summary = "Update product details", description = "Accessible by ADMIN and MANAGER.")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        ProductResponse response = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", response));
    }

    @PatchMapping("/{id}/availability")
    @PreAuthorize("hasAuthority('PRODUCT_UPDATE')")
    @Operation(summary = "Toggle product availability status", description = "Accessible by ADMIN and MANAGER.")
    public ResponseEntity<ApiResponse<ProductResponse>> toggleAvailability(@PathVariable Long id) {
        ProductResponse response = productService.toggleAvailability(id);
        return ResponseEntity.ok(ApiResponse.success("Product availability toggled successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUCT_DELETE')")
    @Operation(summary = "Delete product by ID", description = "Accessible by ADMIN only.")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully", null));
    }
}

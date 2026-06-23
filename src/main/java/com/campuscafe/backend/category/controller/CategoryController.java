package com.campuscafe.backend.category.controller;

import com.campuscafe.backend.category.dto.CategoryResponse;
import com.campuscafe.backend.category.dto.CreateCategoryRequest;
import com.campuscafe.backend.category.dto.UpdateCategoryRequest;
import com.campuscafe.backend.category.service.CategoryService;
import com.campuscafe.backend.common.ApiResponse;
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
@RequestMapping("/categories")
@RequiredArgsConstructor
@Validated
@Tag(name = "Category Management", description = "Endpoints for managing product categories with tenant isolation")
@SecurityRequirement(name = "bearerAuth")
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @PreAuthorize("hasAuthority('CATEGORY_CREATE')")
    @Operation(summary = "Create a new category", description = "Accessible by ADMIN and MANAGER.")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        CategoryResponse response = categoryService.createCategory(request);
        return ResponseEntity.ok(ApiResponse.success("Category created successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CATEGORY_VIEW')")
    @Operation(summary = "Get all categories of the authenticated merchant", description = "Accessible by ADMIN, MANAGER, and CASHIER.")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        List<CategoryResponse> response = categoryService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success("Categories retrieved successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CATEGORY_VIEW')")
    @Operation(summary = "Get detailed category by ID", description = "Accessible by ADMIN, MANAGER, and CASHIER.")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable Long id) {
        CategoryResponse response = categoryService.getCategoryById(id);
        return ResponseEntity.ok(ApiResponse.success("Category retrieved successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CATEGORY_UPDATE')")
    @Operation(summary = "Update category details", description = "Accessible by ADMIN and MANAGER.")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCategoryRequest request
    ) {
        CategoryResponse response = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(ApiResponse.success("Category updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CATEGORY_DELETE')")
    @Operation(summary = "Delete category by ID", description = "Accessible by ADMIN only.")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Category deleted successfully", null));
    }
}

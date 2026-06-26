package com.campuscafe.backend.product.controller;

import com.campuscafe.backend.common.ApiResponse;
import com.campuscafe.backend.product.dto.RecipeRequest;
import com.campuscafe.backend.product.dto.RecipeResponse;
import com.campuscafe.backend.product.service.ProductRecipeService;
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
@RequestMapping("/products/{productId}/recipes")
@RequiredArgsConstructor
@Validated
@Tag(name = "Product Recipe Management", description = "Endpoints for managing recipes and ingredient mappings for products")
@SecurityRequirement(name = "bearerAuth")
public class ProductRecipeController {

    private final ProductRecipeService productRecipeService;

    @PostMapping
    @PreAuthorize("hasAuthority('RECIPE_CREATE')")
    @Operation(summary = "Add an ingredient recipe item to a product", description = "Accessible by ADMIN and MANAGER.")
    public ResponseEntity<ApiResponse<RecipeResponse>> addRecipeItem(
            @PathVariable Long productId,
            @Valid @RequestBody RecipeRequest request
    ) {
        RecipeResponse response = productRecipeService.addRecipeItem(productId, request);
        return ResponseEntity.ok(ApiResponse.success("Recipe item added successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('RECIPE_VIEW')")
    @Operation(summary = "Get the ingredient recipe for a product", description = "Accessible by ADMIN and MANAGER.")
    public ResponseEntity<ApiResponse<List<RecipeResponse>>> getRecipe(@PathVariable Long productId) {
        List<RecipeResponse> response = productRecipeService.getRecipe(productId);
        return ResponseEntity.ok(ApiResponse.success("Recipe retrieved successfully", response));
    }

    @PutMapping("/{recipeId}")
    @PreAuthorize("hasAuthority('RECIPE_UPDATE')")
    @Operation(summary = "Update a recipe item details", description = "Accessible by ADMIN and MANAGER.")
    public ResponseEntity<ApiResponse<RecipeResponse>> updateRecipe(
            @PathVariable Long productId,
            @PathVariable Long recipeId,
            @Valid @RequestBody RecipeRequest request
    ) {
        RecipeResponse response = productRecipeService.updateRecipe(productId, recipeId, request);
        return ResponseEntity.ok(ApiResponse.success("Recipe item updated successfully", response));
    }

    @DeleteMapping("/{recipeId}")
    @PreAuthorize("hasAuthority('RECIPE_DELETE')")
    @Operation(summary = "Delete a recipe item from a product", description = "Accessible by ADMIN and MANAGER.")
    public ResponseEntity<ApiResponse<Void>> deleteRecipe(
            @PathVariable Long productId,
            @PathVariable Long recipeId
    ) {
        productRecipeService.deleteRecipe(productId, recipeId);
        return ResponseEntity.ok(ApiResponse.success("Recipe item deleted successfully", null));
    }
}

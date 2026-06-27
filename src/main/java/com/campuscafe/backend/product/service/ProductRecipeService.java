package com.campuscafe.backend.product.service;

import com.campuscafe.backend.domain.merchant.Merchant;
import com.campuscafe.backend.domain.product.Product;
import com.campuscafe.backend.domain.product.ProductRecipe;
import com.campuscafe.backend.domain.inventory.InventoryItem;
import com.campuscafe.backend.exception.*;
import com.campuscafe.backend.product.dto.RecipeRequest;
import com.campuscafe.backend.product.dto.RecipeResponse;
import com.campuscafe.backend.product.repository.ProductRecipeRepository;
import com.campuscafe.backend.product.repository.ProductRepository;
import com.campuscafe.backend.inventory.repository.InventoryItemRepository;
import com.campuscafe.backend.repository.MerchantRepository;
import com.campuscafe.backend.security.service.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductRecipeService {

    private final ProductRecipeRepository productRecipeRepository;
    private final ProductRepository productRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final MerchantRepository merchantRepository;

    private CustomUserDetails getAuthenticatedUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            throw new AccessDeniedException("User is not authenticated");
        }
        return (CustomUserDetails) authentication.getPrincipal();
    }

    private Product validateAndGetProduct(Long productId, Long merchantId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + productId));
        if (!product.getMerchant().getId().equals(merchantId)) {
            throw new AccessDeniedException("You do not have access to this product");
        }
        return product;
    }

    private InventoryItem validateAndGetInventoryItem(Long itemId, Long merchantId) {
        InventoryItem item = inventoryItemRepository.findById(itemId)
                .orElseThrow(() -> new InventoryItemNotFoundException("Inventory item not found with id: " + itemId));
        if (!item.getMerchant().getId().equals(merchantId)) {
            throw new AccessDeniedException("You do not have access to this inventory item");
        }
        return item;
    }

    public RecipeResponse addRecipeItem(Long productId, RecipeRequest request) {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        Product product = validateAndGetProduct(productId, merchantId);
        InventoryItem inventoryItem = validateAndGetInventoryItem(request.getInventoryItemId(), merchantId);

        if (productRecipeRepository.existsByProductIdAndInventoryItemId(productId, request.getInventoryItemId())) {
            throw new DuplicateInventoryItemException("Recipe item already exists for this product with inventory item: " + inventoryItem.getName());
        }

        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new AccessDeniedException("Merchant not found"));

        ProductRecipe recipe = ProductRecipe.builder()
                .merchant(merchant)
                .product(product)
                .inventoryItem(inventoryItem)
                .quantityRequired(request.getQuantityRequired())
                .build();

        ProductRecipe saved = productRecipeRepository.save(recipe);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<RecipeResponse> getRecipe(Long productId) {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        validateAndGetProduct(productId, merchantId);

        List<ProductRecipe> recipes = productRecipeRepository.findByProductIdAndMerchantId(productId, merchantId);
        return recipes.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public RecipeResponse updateRecipe(Long productId, Long recipeId, RecipeRequest request) {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        validateAndGetProduct(productId, merchantId);
        ProductRecipe recipe = productRecipeRepository.findByIdAndProductIdAndMerchantId(recipeId, productId, merchantId)
                .orElseThrow(() -> new RecipeNotFoundException("Recipe item not found with id: " + recipeId));

        InventoryItem inventoryItem = validateAndGetInventoryItem(request.getInventoryItemId(), merchantId);

        if (productRecipeRepository.existsByProductIdAndInventoryItemIdAndIdNot(productId, request.getInventoryItemId(), recipeId)) {
            throw new DuplicateInventoryItemException("Recipe item already exists for this product with inventory item: " + inventoryItem.getName());
        }

        recipe.setInventoryItem(inventoryItem);
        recipe.setQuantityRequired(request.getQuantityRequired());

        ProductRecipe updated = productRecipeRepository.save(recipe);
        return mapToResponse(updated);
    }

    public void deleteRecipe(Long productId, Long recipeId) {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        validateAndGetProduct(productId, merchantId);
        ProductRecipe recipe = productRecipeRepository.findByIdAndProductIdAndMerchantId(recipeId, productId, merchantId)
                .orElseThrow(() -> new RecipeNotFoundException("Recipe item not found with id: " + recipeId));

        productRecipeRepository.delete(recipe);
    }

    private RecipeResponse mapToResponse(ProductRecipe recipe) {
        return RecipeResponse.builder()
                .id(recipe.getId())
                .merchantId(recipe.getMerchant().getId())
                .productId(recipe.getProduct().getId())
                .inventoryItemId(recipe.getInventoryItem().getId())
                .inventoryItemName(recipe.getInventoryItem().getName())
                .unit(recipe.getInventoryItem().getUnit())
                .quantityRequired(recipe.getQuantityRequired())
                .createdAt(recipe.getCreatedAt())
                .build();
    }
}

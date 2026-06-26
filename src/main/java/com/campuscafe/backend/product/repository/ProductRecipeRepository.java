package com.campuscafe.backend.product.repository;

import com.campuscafe.backend.domain.product.ProductRecipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRecipeRepository extends JpaRepository<ProductRecipe, Long> {
    List<ProductRecipe> findByProductId(Long productId);
    List<ProductRecipe> findByProductIdAndMerchantId(Long productId, Long merchantId);
    Optional<ProductRecipe> findByIdAndProductIdAndMerchantId(Long id, Long productId, Long merchantId);
    boolean existsByProductIdAndInventoryItemId(Long productId, Long inventoryItemId);
    boolean existsByProductIdAndInventoryItemIdAndIdNot(Long productId, Long inventoryItemId, Long id);
}

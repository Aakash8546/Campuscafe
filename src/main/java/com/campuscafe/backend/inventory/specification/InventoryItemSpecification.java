package com.campuscafe.backend.inventory.specification;

import com.campuscafe.backend.domain.inventory.InventoryItem;
import org.springframework.data.jpa.domain.Specification;

public class InventoryItemSpecification {

    public static Specification<InventoryItem> withMerchantId(Long merchantId) {
        return (root, query, cb) -> cb.equal(root.get("merchant").get("id"), merchantId);
    }

    public static Specification<InventoryItem> withName(String name) {
        return (root, query, cb) -> {
            if (name == null || name.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
        };
    }

    public static Specification<InventoryItem> withCategoryId(Long categoryId) {
        return (root, query, cb) -> {
            if (categoryId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("category").get("id"), categoryId);
        };
    }

    public static Specification<InventoryItem> withLowStock(Boolean lowStock) {
        return (root, query, cb) -> {
            if (lowStock == null || !lowStock) {
                return cb.conjunction();
            }
            return cb.lessThanOrEqualTo(root.get("currentStock"), root.get("minStock"));
        };
    }
}

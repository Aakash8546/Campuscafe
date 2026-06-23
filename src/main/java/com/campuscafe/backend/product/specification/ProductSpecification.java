package com.campuscafe.backend.product.specification;

import com.campuscafe.backend.domain.product.Product;
import org.springframework.data.jpa.domain.Specification;

public class ProductSpecification {

    public static Specification<Product> withMerchantId(Long merchantId) {
        return (root, query, cb) -> cb.equal(root.get("merchant").get("id"), merchantId);
    }

    public static Specification<Product> withName(String name) {
        return (root, query, cb) -> {
            if (name == null || name.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
        };
    }

    public static Specification<Product> withCategoryId(Long categoryId) {
        return (root, query, cb) -> {
            if (categoryId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("category").get("id"), categoryId);
        };
    }

    public static Specification<Product> withAvailable(Boolean available) {
        return (root, query, cb) -> {
            if (available == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("available"), available);
        };
    }
}

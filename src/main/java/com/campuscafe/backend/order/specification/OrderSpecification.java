package com.campuscafe.backend.order.specification;

import com.campuscafe.backend.domain.order.Order;
import com.campuscafe.backend.domain.order.enums.OrderSource;
import com.campuscafe.backend.domain.order.enums.OrderStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public class OrderSpecification {

    public static Specification<Order> withMerchantId(Long merchantId) {
        return (root, query, cb) -> cb.equal(root.get("merchant").get("id"), merchantId);
    }

    public static Specification<Order> withStatus(OrderStatus status) {
        return (root, query, cb) -> {
            if (status == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<Order> withSource(OrderSource source) {
        return (root, query, cb) -> {
            if (source == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("source"), source);
        };
    }

    public static Specification<Order> withOrderNumber(String orderNumber) {
        return (root, query, cb) -> {
            if (orderNumber == null || orderNumber.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.equal(root.get("orderNumber"), orderNumber.trim());
        };
    }

    public static Specification<Order> withDateRange(Instant startDate, Instant endDate) {
        return (root, query, cb) -> {
            if (startDate == null && endDate == null) {
                return cb.conjunction();
            }
            if (startDate != null && endDate != null) {
                return cb.between(root.get("createdAt"), startDate, endDate);
            }
            if (startDate != null) {
                return cb.greaterThanOrEqualTo(root.get("createdAt"), startDate);
            }
            return cb.lessThanOrEqualTo(root.get("createdAt"), endDate);
        };
    }
}

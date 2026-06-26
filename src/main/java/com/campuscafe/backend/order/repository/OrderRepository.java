package com.campuscafe.backend.order.repository;

import com.campuscafe.backend.domain.order.Order;
import com.campuscafe.backend.domain.order.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    Optional<Order> findByIdAndMerchantId(Long id, Long merchantId);

    boolean existsByOrderNumberAndMerchantId(String orderNumber, Long merchantId);

    List<Order> findByMerchantId(Long merchantId);

    long countByMerchantIdAndCreatedAtBetween(Long merchantId, Instant start, Instant end);

    long countByMerchantIdAndStatus(Long merchantId, OrderStatus status);

    long countByMerchantIdAndStatusAndCreatedAtBetween(Long merchantId, OrderStatus status, Instant start, Instant end);

    @Query("SELECT SUM(o.finalAmount) FROM Order o WHERE o.merchant.id = :merchantId AND o.status = :status AND o.createdAt >= :start AND o.createdAt < :end")
    BigDecimal sumRevenueByMerchantIdAndStatusAndCreatedAtBetween(
            @Param("merchantId") Long merchantId,
            @Param("status") OrderStatus status,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    @Query(value = "SELECT nextval('order_number_sequence')", nativeQuery = true)
    Long getNextOrderNumberSequence();

    @Query("SELECT o FROM Order o WHERE o.merchant.id = :merchantId AND o.status IN :statuses " +
           "ORDER BY CASE o.priority " +
           "  WHEN com.campuscafe.backend.domain.order.enums.OrderPriority.URGENT THEN 1 " +
           "  WHEN com.campuscafe.backend.domain.order.enums.OrderPriority.HIGH THEN 2 " +
           "  WHEN com.campuscafe.backend.domain.order.enums.OrderPriority.MEDIUM THEN 3 " +
           "  WHEN com.campuscafe.backend.domain.order.enums.OrderPriority.LOW THEN 4 " +
           "  ELSE 5 END ASC, o.createdAt ASC")
    List<Order> findActiveOrdersSorted(
            @Param("merchantId") Long merchantId,
            @Param("statuses") List<OrderStatus> statuses
    );
}

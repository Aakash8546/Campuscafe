package com.campuscafe.backend.dashboard.repository;

import com.campuscafe.backend.domain.order.Order;
import com.campuscafe.backend.domain.order.enums.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Repository
public interface DashboardOrderRepository extends JpaRepository<Order, Long> {

    long countByMerchantIdAndCreatedAtBetween(Long merchantId, Instant start, Instant end);

    long countByMerchantIdAndStatusAndCreatedAtBetween(Long merchantId, OrderStatus status, Instant start, Instant end);

    @Query("SELECT COALESCE(SUM(o.finalAmount), 0) FROM Order o WHERE o.merchant.id = :merchantId AND o.status = :status AND o.createdAt >= :start AND o.createdAt < :end")
    BigDecimal sumRevenueByMerchantIdAndStatusAndCreatedAtBetween(
            @Param("merchantId") Long merchantId,
            @Param("status") OrderStatus status,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    List<Order> findTop10ByMerchantIdOrderByCreatedAtDesc(Long merchantId);

    @Query("SELECT CAST(o.createdAt AS date) as date, COALESCE(SUM(o.finalAmount), 0) as revenue, COUNT(o) as orders " +
           "FROM Order o " +
           "WHERE o.merchant.id = :merchantId AND o.status = :status AND o.createdAt >= :start AND o.createdAt <= :end " +
           "GROUP BY CAST(o.createdAt AS date) " +
           "ORDER BY CAST(o.createdAt AS date) ASC")
    List<Object[]> getSalesOverview(
            @Param("merchantId") Long merchantId,
            @Param("status") OrderStatus status,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    @Query("SELECT p.id as productId, p.name as productName, SUM(oi.quantity) as quantitySold, SUM(oi.subtotal) as revenue " +
           "FROM OrderItem oi " +
           "JOIN oi.order o " +
           "JOIN oi.product p " +
           "WHERE o.merchant.id = :merchantId AND o.status = :status " +
           "GROUP BY p.id, p.name " +
           "ORDER BY SUM(oi.quantity) DESC")
    List<Object[]> getTopSellingProducts(
            @Param("merchantId") Long merchantId,
            @Param("status") OrderStatus status,
            Pageable pageable
    );

    @Query("SELECT o.status, COUNT(o) FROM Order o WHERE o.merchant.id = :merchantId GROUP BY o.status")
    List<Object[]> getOrderStatusDistribution(@Param("merchantId") Long merchantId);
}

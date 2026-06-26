package com.campuscafe.backend.report.repository;

import com.campuscafe.backend.domain.order.OrderItem;
import com.campuscafe.backend.domain.order.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ReportOrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("SELECT p.name as productName, SUM(oi.quantity) as quantitySold, SUM(oi.subtotal) as revenue " +
           "FROM OrderItem oi " +
           "JOIN oi.order o " +
           "JOIN oi.product p " +
           "WHERE o.merchant.id = :merchantId AND o.status = :status AND o.createdAt >= :start AND o.createdAt <= :end " +
           "GROUP BY p.name " +
           "ORDER BY SUM(oi.subtotal) DESC")
    List<Object[]> getProductPerformanceReport(
            @Param("merchantId") Long merchantId,
            @Param("status") OrderStatus status,
            @Param("start") Instant start,
            @Param("end") Instant end
    );
}

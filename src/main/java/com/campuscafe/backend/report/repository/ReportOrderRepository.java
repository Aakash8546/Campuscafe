package com.campuscafe.backend.report.repository;

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

@Repository
public interface ReportOrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    @Query("SELECT COALESCE(SUM(o.subtotal), 0), COALESCE(SUM(o.discountAmount), 0), COALESCE(SUM(o.finalAmount), 0) " +
           "FROM Order o " +
           "WHERE o.merchant.id = :merchantId AND o.status = :status AND o.createdAt >= :start AND o.createdAt <= :end")
    List<Object[]> getRevenueReportMetrics(
            @Param("merchantId") Long merchantId,
            @Param("status") OrderStatus status,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    long countByMerchantIdAndStatusAndCreatedAtBetween(Long merchantId, OrderStatus status, Instant start, Instant end);

    @Query("SELECT COALESCE(SUM(o.finalAmount), 0) FROM Order o WHERE o.merchant.id = :merchantId AND o.status = :status AND o.createdAt >= :start AND o.createdAt < :end")
    BigDecimal sumRevenueByMerchantIdAndStatusAndCreatedAtBetween(
            @Param("merchantId") Long merchantId,
            @Param("status") OrderStatus status,
            @Param("start") Instant start,
            @Param("end") Instant end
    );
}

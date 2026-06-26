package com.campuscafe.backend.report.repository;

import com.campuscafe.backend.domain.inventory.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportInventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    long countByMerchantId(Long merchantId);

    @Query("SELECT COUNT(i) FROM InventoryItem i WHERE i.merchant.id = :merchantId AND i.currentStock <= i.minStock")
    long countLowStockItemsByMerchantId(@Param("merchantId") Long merchantId);

    @Query("SELECT COUNT(i) FROM InventoryItem i WHERE i.merchant.id = :merchantId AND i.currentStock = 0")
    long countOutOfStockItemsByMerchantId(@Param("merchantId") Long merchantId);
}

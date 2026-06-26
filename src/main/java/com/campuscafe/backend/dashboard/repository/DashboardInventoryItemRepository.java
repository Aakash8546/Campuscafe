package com.campuscafe.backend.dashboard.repository;

import com.campuscafe.backend.domain.inventory.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DashboardInventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    @Query("SELECT COUNT(i) FROM InventoryItem i WHERE i.merchant.id = :merchantId AND i.currentStock <= i.minStock")
    long countLowStockItemsByMerchantId(@Param("merchantId") Long merchantId);

    @Query("SELECT i FROM InventoryItem i WHERE i.merchant.id = :merchantId AND i.currentStock <= i.minStock")
    List<InventoryItem> findLowStockItemsByMerchantId(@Param("merchantId") Long merchantId);
}

package com.campuscafe.backend.inventory.repository;

import com.campuscafe.backend.domain.inventory.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long>, JpaSpecificationExecutor<InventoryItem> {

    Optional<InventoryItem> findByIdAndMerchantId(Long id, Long merchantId);

    boolean existsByNameAndMerchantId(String name, Long merchantId);

    boolean existsByNameAndMerchantIdAndIdNot(String name, Long merchantId, Long id);

    long countByMerchantId(Long merchantId);

    @Query("SELECT COUNT(i) FROM InventoryItem i WHERE i.merchant.id = :merchantId AND i.currentStock <= i.minStock")
    long countLowStockItems(@Param("merchantId") Long merchantId);
}

package com.campuscafe.backend.inventory.repository;

import com.campuscafe.backend.domain.inventory.InventoryTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long>, JpaSpecificationExecutor<InventoryTransaction> {

    Page<InventoryTransaction> findByInventoryItemMerchantId(Long merchantId, Pageable pageable);
}

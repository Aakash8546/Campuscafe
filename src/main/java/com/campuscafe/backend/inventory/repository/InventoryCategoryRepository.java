package com.campuscafe.backend.inventory.repository;

import com.campuscafe.backend.domain.inventory.InventoryCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryCategoryRepository extends JpaRepository<InventoryCategory, Long> {

    List<InventoryCategory> findByMerchantId(Long merchantId);

    Optional<InventoryCategory> findByIdAndMerchantId(Long id, Long merchantId);

    boolean existsByNameAndMerchantId(String name, Long merchantId);

    boolean existsByNameAndMerchantIdAndIdNot(String name, Long merchantId, Long id);

    long countByMerchantId(Long merchantId);
}

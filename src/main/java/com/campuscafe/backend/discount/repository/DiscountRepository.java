package com.campuscafe.backend.discount.repository;

import com.campuscafe.backend.domain.discount.Discount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiscountRepository extends JpaRepository<Discount, Long> {

    List<Discount> findByMerchantId(Long merchantId);

    Optional<Discount> findByIdAndMerchantId(Long id, Long merchantId);

    boolean existsByMerchantIdAndNameIgnoreCase(Long merchantId, String name);

    boolean existsByMerchantIdAndNameIgnoreCaseAndIdNot(Long merchantId, String name, Long id);
}

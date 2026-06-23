package com.campuscafe.backend.category.repository;

import com.campuscafe.backend.domain.product.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByMerchantId(Long merchantId);

    Optional<Category> findByIdAndMerchantId(Long id, Long merchantId);

    boolean existsByNameAndMerchantId(String name, Long merchantId);

    boolean existsByNameAndMerchantIdAndIdNot(String name, Long merchantId, Long id);
}

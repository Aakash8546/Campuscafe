package com.campuscafe.backend.product.repository;

import com.campuscafe.backend.domain.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByIdAndMerchantId(Long id, Long merchantId);

    boolean existsByNameAndMerchantId(String name, Long merchantId);

    boolean existsByNameAndMerchantIdAndIdNot(String name, Long merchantId, Long id);
}

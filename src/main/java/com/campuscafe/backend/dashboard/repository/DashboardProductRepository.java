package com.campuscafe.backend.dashboard.repository;

import com.campuscafe.backend.domain.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DashboardProductRepository extends JpaRepository<Product, Long> {
    long countByMerchantId(Long merchantId);
}

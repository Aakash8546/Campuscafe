package com.campuscafe.backend.repository;

import com.campuscafe.backend.domain.merchant.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, Long> {
    Optional<Merchant> findByEmail(String email);
    boolean existsByEmail(String email);
}

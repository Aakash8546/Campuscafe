package com.campuscafe.backend.repository;

import com.campuscafe.backend.domain.merchant.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, Long> {
    Optional<Merchant> findByEmail(String email);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT m FROM Merchant m WHERE m.id = :id")
    Optional<Merchant> findAndLockById(@org.springframework.data.repository.query.Param("id") Long id);

    boolean existsByEmail(String email);
}

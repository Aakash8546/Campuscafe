package com.campuscafe.backend.repository;

import com.campuscafe.backend.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByMerchantId(Long merchantId);
    Optional<User> findByIdAndMerchantId(Long id, Long merchantId);
}

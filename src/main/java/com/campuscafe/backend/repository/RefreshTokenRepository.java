package com.campuscafe.backend.repository;

import com.campuscafe.backend.domain.user.RefreshToken;
import com.campuscafe.backend.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    List<RefreshToken> findByUserAndRevoked(User user, boolean revoked);
}

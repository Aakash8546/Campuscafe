package com.campuscafe.backend.repository;

import com.campuscafe.backend.domain.user.VerificationToken;
import com.campuscafe.backend.domain.user.enums.VerificationPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByEmailAndOtpAndPurpose(String email, String otp, VerificationPurpose purpose);

    @Modifying
    @Query("UPDATE VerificationToken vt SET vt.expiresAt = :now WHERE vt.email = :email AND vt.purpose = :purpose AND vt.expiresAt > :now")
    void invalidateActiveTokens(@Param("email") String email, @Param("purpose") VerificationPurpose purpose, @Param("now") Instant now);
}

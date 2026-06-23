package com.campuscafe.backend.repository;

import com.campuscafe.backend.domain.user.VerificationToken;
import com.campuscafe.backend.domain.user.enums.VerificationPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByEmailAndOtpAndPurpose(String email, String otp, VerificationPurpose purpose);
}

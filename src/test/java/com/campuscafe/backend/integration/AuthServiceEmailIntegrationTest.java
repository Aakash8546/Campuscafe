package com.campuscafe.backend.integration;

import com.campuscafe.backend.auth.dto.ForgotPasswordRequest;
import com.campuscafe.backend.auth.dto.SignupRequest;
import com.campuscafe.backend.auth.service.AuthService;
import com.campuscafe.backend.domain.user.enums.VerificationPurpose;
import com.campuscafe.backend.mail.exception.EmailSendFailedException;
import com.campuscafe.backend.mail.service.EmailService;
import com.campuscafe.backend.repository.MerchantRepository;
import com.campuscafe.backend.repository.UserRepository;
import com.campuscafe.backend.repository.VerificationTokenRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
    "mail.smtp.username=mock-username",
    "mail.smtp.password=mock-password",
    "mail.smtp.from=mock@example.com",
    "mail.smtp.from-name=Mock Sender",
    "mail.smtp.otp-expiry-minutes=5",
    "spring.datasource.url=jdbc:postgresql://localhost:5432/campus_cafe",
    "spring.datasource.username=postgres",
    "spring.datasource.password=postgres"
})
class AuthServiceEmailIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private com.campuscafe.backend.repository.RoleRepository roleRepository;

    @Autowired
    private VerificationTokenRepository tokenRepository;

    @MockBean
    private EmailService emailService;

    @BeforeEach
    @AfterEach
    void cleanup() {
        tokenRepository.findAll().stream()
                .filter(t -> "rollback@test.com".equals(t.getEmail()) || "existing-user@test.com".equals(t.getEmail()))
                .forEach(tokenRepository::delete);

        userRepository.findByEmail("rollback@test.com").ifPresent(userRepository::delete);
        userRepository.findByEmail("existing-user@test.com").ifPresent(userRepository::delete);

        merchantRepository.findByEmail("rollback@test.com").ifPresent(merchantRepository::delete);
        merchantRepository.findByEmail("existing-user@test.com").ifPresent(merchantRepository::delete);
    }

    @Test
    void testSignupRollbackOnEmailFailure() {
        // Arrange
        SignupRequest request = SignupRequest.builder()
                .cafeName("Rollback Cafe")
                .email("rollback@test.com")
                .phone("1234567890")
                .password("Password@123")
                .build();

        // Mock EmailService to throw exception when sending email
        doThrow(new EmailSendFailedException("Email sending failed"))
                .when(emailService).sendOtpEmail(eq("rollback@test.com"), anyString(), anyString());

        // Act & Assert
        assertThrows(EmailSendFailedException.class, () -> authService.signup(request));

        // Assert database rollback: User, Merchant, and Token must NOT exist
        assertFalse(userRepository.existsByEmail("rollback@test.com"));
        assertTrue(merchantRepository.findByEmail("rollback@test.com").isEmpty());
        assertTrue(tokenRepository.findByEmailAndOtpAndPurpose("rollback@test.com", "", VerificationPurpose.EMAIL_VERIFICATION).isEmpty());
    }

    @Test
    void testForgotPasswordRollbackOnEmailFailure() {
        // Arrange
        String email = "existing-user@test.com";
        
        // Seed database with a user/merchant so forgotPassword doesn't throw UserNotFoundException
        com.campuscafe.backend.domain.merchant.Merchant merchant = com.campuscafe.backend.domain.merchant.Merchant.builder()
                .cafeName("Existing Cafe")
                .email(email)
                .phone("9876543210")
                .verified(true)
                .active(true)
                .build();
        merchant = merchantRepository.save(merchant);

        com.campuscafe.backend.domain.user.Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new RuntimeException("ADMIN role not seeded"));

        com.campuscafe.backend.domain.user.User user = com.campuscafe.backend.domain.user.User.builder()
                .merchant(merchant)
                .role(adminRole)
                .name("Test User")
                .email(email)
                .phone("9876543210")
                .password("encodedPassword")
                .active(true)
                .build();
        userRepository.save(user);

        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email(email)
                .build();

        // Mock EmailService to throw exception when sending email
        doThrow(new EmailSendFailedException("Email sending failed"))
                .when(emailService).sendOtpEmail(eq(email), anyString(), anyString());

        // Act & Assert
        assertThrows(EmailSendFailedException.class, () -> authService.forgotPassword(request));

        // Assert database rollback: Token must NOT be stored
        var tokens = tokenRepository.findByEmailAndOtpAndPurpose(email, "", VerificationPurpose.PASSWORD_RESET);
        assertTrue(tokens.isEmpty());
    }
}

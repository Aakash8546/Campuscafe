package com.campuscafe.backend.auth.service;

import com.campuscafe.backend.auth.dto.*;
import com.campuscafe.backend.domain.merchant.Merchant;
import com.campuscafe.backend.domain.user.Role;
import com.campuscafe.backend.domain.user.User;
import com.campuscafe.backend.domain.user.VerificationToken;
import com.campuscafe.backend.domain.user.enums.VerificationPurpose;
import com.campuscafe.backend.exception.EmailAlreadyExistsException;
import com.campuscafe.backend.repository.*;
import com.campuscafe.backend.security.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private MerchantRepository merchantRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private VerificationTokenRepository verificationTokenRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private Environment environment;
    @Mock
    private EmailService emailService;
    @Mock
    private EmailProperties emailProperties;

    @InjectMocks
    private AuthService authService;

    private SignupRequest signupRequest;
    private Role adminRole;
    private Merchant merchant;

    @BeforeEach
    void setUp() {
        lenient().when(emailProperties.getOtpExpiryMinutes()).thenReturn(5);
        signupRequest = SignupRequest.builder()
                .cafeName("Test Cafe")
                .email("admin@test.com")
                .phone("9876543210")
                .password("Password@123")
                .build();

        adminRole = Role.builder().id(1L).name("ADMIN").build();
        merchant = Merchant.builder().cafeName("Test Cafe").email("admin@test.com").verified(false).build();
        merchant.setId(1L);
    }

    @Test
    void testSignup_Success() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(merchantRepository.save(any(Merchant.class))).thenReturn(merchant);
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});

        Map<String, String> result = authService.signup(signupRequest);

        assertNotNull(result);
        assertTrue(result.containsKey("otp"));
        assertEquals(6, result.get("otp").length());

        verify(merchantRepository, times(1)).save(any(Merchant.class));
        verify(userRepository, times(1)).save(any(User.class));
        verify(verificationTokenRepository, times(1)).save(any(VerificationToken.class));
    }

    @Test
    void testSignup_EmailAlreadyExists() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> authService.signup(signupRequest));

        verify(merchantRepository, never()).save(any(Merchant.class));
    }

    @Test
    void testVerifyOtp_Success() {
        VerificationToken token = VerificationToken.builder()
                .id(1L)
                .email("admin@test.com")
                .otp("123456")
                .purpose(VerificationPurpose.EMAIL_VERIFICATION)
                .expiresAt(Instant.now().plusSeconds(600))
                .verified(false)
                .build();

        User user = User.builder()
                .email("admin@test.com")
                .merchant(merchant)
                .build();
        user.setId(1L);

        when(verificationTokenRepository.findByEmailAndOtpAndPurpose(anyString(), anyString(), any(VerificationPurpose.class)))
                .thenReturn(Optional.of(token));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

        VerifyOtpRequest request = VerifyOtpRequest.builder().email("admin@test.com").otp("123456").build();
        authService.verifyOtp(request);

        assertTrue(token.getVerified());
        assertTrue(merchant.getVerified());
    }
}

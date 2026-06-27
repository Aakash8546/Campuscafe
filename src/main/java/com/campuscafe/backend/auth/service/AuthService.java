package com.campuscafe.backend.auth.service;

import com.campuscafe.backend.auth.dto.*;
import com.campuscafe.backend.domain.merchant.Merchant;
import com.campuscafe.backend.domain.user.RefreshToken;
import com.campuscafe.backend.domain.user.Role;
import com.campuscafe.backend.domain.user.User;
import com.campuscafe.backend.domain.user.VerificationToken;
import com.campuscafe.backend.domain.user.enums.VerificationPurpose;
import com.campuscafe.backend.exception.*;
import com.campuscafe.backend.repository.*;
import com.campuscafe.backend.security.service.CustomUserDetails;
import com.campuscafe.backend.security.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.campuscafe.backend.mail.config.EmailProperties;
import com.campuscafe.backend.mail.service.EmailService;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final MerchantRepository merchantRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Environment environment;
    private final EmailService emailService;
    private final EmailProperties emailProperties;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public Map<String, String> signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists: " + request.getEmail());
        }


        Merchant merchant = Merchant.builder()
                .cafeName(request.getCafeName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .verified(false)
                .active(true)
                .build();
        merchant = merchantRepository.save(merchant);


        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new RuntimeException("ADMIN role not seeded in system"));


        User user = User.builder()
                .merchant(merchant)
                .role(adminRole)
                .name(request.getCafeName() + " Admin")
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .active(true)
                .build();
        userRepository.save(user);


        String otp = generateOtp();


        VerificationToken token = VerificationToken.builder()
                .email(request.getEmail())
                .otp(otp)
                .purpose(VerificationPurpose.EMAIL_VERIFICATION)
                .expiresAt(Instant.now().plusSeconds(emailProperties.getOtpExpiryMinutes() * 60L))
                .verified(false)
                .build();
        verificationTokenRepository.save(token);

        emailService.sendOtpEmail(request.getEmail(), otp, "EMAIL_VERIFICATION");

        if (isDevProfile()) {
            return Collections.singletonMap("otp", otp);
        }
        return Collections.emptyMap();
    }

    @Transactional
    public void verifyOtp(VerifyOtpRequest request) {
        VerificationToken token = verificationTokenRepository.findByEmailAndOtpAndPurpose(
                request.getEmail(), request.getOtp(), VerificationPurpose.EMAIL_VERIFICATION
        ).orElseThrow(() -> new OtpInvalidException("Invalid OTP"));

        if (token.getVerified()) {
            throw new OtpInvalidException("OTP already verified");
        }

        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new OtpExpiredException("OTP has expired");
        }

        token.setVerified(true);
        verificationTokenRepository.save(token);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + request.getEmail()));

        Merchant merchant = user.getMerchant();
        merchant.setVerified(true);
        merchantRepository.save(merchant);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + request.getEmail()));

        if (!user.getMerchant().getVerified()) {
            throw new MerchantNotVerifiedException("Merchant account is not verified");
        }

        if (!user.getActive() || !user.getMerchant().getActive()) {
            throw new InvalidCredentialsException("Account is deactivated");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshTokenString = jwtService.generateRefreshToken(userDetails);

        // Store refresh token in DB
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenString)
                .expiresAt(Instant.now().plusSeconds(604800)) // 7 days
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenString)
                .role(user.getRole().getName())
                .merchantId(user.getMerchant().getId())
                .build();
    }

    @Transactional(readOnly = true)
    public Map<String, String> refreshToken(RefreshTokenRequest request) {
        RefreshToken token = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new RefreshTokenInvalidException("Invalid refresh token"));

        if (token.getRevoked()) {
            throw new RefreshTokenInvalidException("Refresh token has been revoked");
        }

        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new RefreshTokenExpiredException("Refresh token has expired");
        }

        CustomUserDetails userDetails = new CustomUserDetails(token.getUser());
        String newAccessToken = jwtService.generateAccessToken(userDetails);

        return Collections.singletonMap("accessToken", newAccessToken);
    }

    @Transactional
    public void logout(LogoutRequest request) {
        RefreshToken token = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new RefreshTokenInvalidException("Invalid refresh token"));

        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }

    @Transactional
    public Map<String, String> forgotPassword(ForgotPasswordRequest request) {
        if (!userRepository.existsByEmail(request.getEmail())) {
            throw new UserNotFoundException("User not found with email: " + request.getEmail());
        }

        String otp = generateOtp();

        VerificationToken token = VerificationToken.builder()
                .email(request.getEmail())
                .otp(otp)
                .purpose(VerificationPurpose.PASSWORD_RESET)
                .expiresAt(Instant.now().plusSeconds(emailProperties.getOtpExpiryMinutes() * 60L))
                .verified(false)
                .build();
        verificationTokenRepository.save(token);

        emailService.sendOtpEmail(request.getEmail(), otp, "PASSWORD_RESET");

        if (isDevProfile()) {
            return Collections.singletonMap("otp", otp);
        }
        return Collections.emptyMap();
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        VerificationToken token = verificationTokenRepository.findByEmailAndOtpAndPurpose(
                request.getEmail(), request.getOtp(), VerificationPurpose.PASSWORD_RESET
        ).orElseThrow(() -> new OtpInvalidException("Invalid OTP"));

        if (token.getVerified()) {
            throw new OtpInvalidException("OTP already verified");
        }

        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new OtpExpiredException("OTP has expired");
        }

        token.setVerified(true);
        verificationTokenRepository.save(token);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + request.getEmail()));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public Map<String, String> resendOtp(ResendOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + request.getEmail()));

        if (request.getPurpose() == VerificationPurpose.EMAIL_VERIFICATION && user.getMerchant().getVerified()) {
            throw new OtpInvalidException("Email is already verified");
        }


        String otp = generateOtp();


        VerificationToken token = VerificationToken.builder()
                .email(request.getEmail())
                .otp(otp)
                .purpose(request.getPurpose())
                .expiresAt(Instant.now().plusSeconds(emailProperties.getOtpExpiryMinutes() * 60L))
                .verified(false)
                .build();
        verificationTokenRepository.save(token);

        emailService.sendOtpEmail(request.getEmail(), otp, request.getPurpose().name());

        if (isDevProfile()) {
            return Collections.singletonMap("otp", otp);
        }
        return Collections.emptyMap();
    }

    private String generateOtp() {
        return String.format("%06d", secureRandom.nextInt(1000000));
    }

    private boolean isDevProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("dev");
    }
}

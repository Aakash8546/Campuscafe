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
import com.campuscafe.backend.security.service.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.campuscafe.backend.mail.config.EmailProperties;
import com.campuscafe.backend.mail.service.EmailService;
import com.campuscafe.backend.domain.user.LoginStatus;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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
    private final LoginAttemptService loginAttemptService;
    private final UserLoginLogService userLoginLogService;

    @Value("${application.security.jwt.refresh-token.expiration:604800000}")
    private long refreshExpiration;

    private final SecureRandom secureRandom = new SecureRandom();

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private void invalidatePreviousOtps(String email, VerificationPurpose purpose) {
        try {
            verificationTokenRepository.invalidateActiveTokens(email, purpose, Instant.now());
        } catch (Exception e) {
            log.error("Failed to invalidate previous active OTPs for email: {} and purpose: {}", email, purpose, e);
        }
    }

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


        invalidatePreviousOtps(request.getEmail(), VerificationPurpose.EMAIL_VERIFICATION);
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
        String ipAddress = getClientIp();
        String userAgent = getUserAgent();

        if (loginAttemptService.isBlocked(request.getEmail())) {
            long remainingMinutes = loginAttemptService.getRemainingLockMinutes(request.getEmail());
            userLoginLogService.recordLog(null, null, request.getEmail(), ipAddress, userAgent, LoginStatus.BLOCKED, "Account temporarily locked");
            throw new AccountLockedException("Account is temporarily locked due to multiple failed login attempts. Please try again after " + remainingMinutes + " minutes.");
        }

        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (user == null) {
            userLoginLogService.recordLog(null, null, request.getEmail(), ipAddress, userAgent, LoginStatus.FAILED, "Invalid credentials");
            throw new InvalidCredentialsException("Invalid email or password");
        }

        Merchant merchant = user.getMerchant();
        if (merchant == null) {
            userLoginLogService.recordLog(null, user, request.getEmail(), ipAddress, userAgent, LoginStatus.FAILED, "Merchant account not found");
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if (!merchant.getVerified()) {
            userLoginLogService.recordLog(merchant, user, request.getEmail(), ipAddress, userAgent, LoginStatus.FAILED, "Merchant account not verified");
            throw new MerchantNotVerifiedException("Merchant account is not verified");
        }

        if (!user.getActive() || !merchant.getActive()) {
            userLoginLogService.recordLog(merchant, user, request.getEmail(), ipAddress, userAgent, LoginStatus.FAILED, "Account is deactivated");
            throw new InvalidCredentialsException("Account is deactivated");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginAttemptService.loginFailed(request.getEmail());
            userLoginLogService.recordLog(merchant, user, request.getEmail(), ipAddress, userAgent, LoginStatus.FAILED, "Invalid credentials");
            throw new InvalidCredentialsException("Invalid email or password");
        }

        loginAttemptService.loginSucceeded(request.getEmail());
        userLoginLogService.recordLog(merchant, user, request.getEmail(), ipAddress, userAgent, LoginStatus.SUCCESS, null);

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshTokenString = jwtService.generateRefreshToken(userDetails);

        // Store hashed refresh token in DB
        String hashedToken = hashToken(refreshTokenString);
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(hashedToken)
                .expiresAt(Instant.now().plusSeconds(refreshExpiration / 1000))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenString)
                .role(user.getRole().getName())
                .merchantId(merchant.getId())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<UserLoginLogResponse> getLoginLogs(Long merchantId, Pageable pageable) {
        return userLoginLogService.getLoginLogs(merchantId, pageable);
    }

    private String getClientIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            String xfHeader = request.getHeader("X-Forwarded-For");
            if (xfHeader == null || xfHeader.isEmpty()) {
                return request.getRemoteAddr();
            }
            return xfHeader.split(",")[0].trim();
        }
        return "127.0.0.1";
    }

    private String getUserAgent() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            String ua = request.getHeader("User-Agent");
            return ua != null ? (ua.length() > 255 ? ua.substring(0, 255) : ua) : "Unknown";
        }
        return "Unknown";
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public Map<String, String> refreshToken(RefreshTokenRequest request) {
        String hashedToken = hashToken(request.getRefreshToken());
        RefreshToken token = refreshTokenRepository.findByToken(hashedToken)
                .orElseThrow(() -> new RefreshTokenInvalidException("Invalid refresh token"));

        if (token.getRevoked()) {
            throw new RefreshTokenInvalidException("Refresh token has been revoked");
        }

        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new RefreshTokenExpiredException("Refresh token has expired");
        }

        // Revoke the old token (Rotation)
        token.setRevoked(true);
        refreshTokenRepository.save(token);

        User user = token.getUser();
        CustomUserDetails userDetails = new CustomUserDetails(user);
        
        // Generate new Access and Refresh tokens
        String newAccessToken = jwtService.generateAccessToken(userDetails);
        String newRefreshTokenString = jwtService.generateRefreshToken(userDetails);

        // Store new hashed refresh token in DB
        String newHashedToken = hashToken(newRefreshTokenString);
        RefreshToken newRefreshToken = RefreshToken.builder()
                .user(user)
                .token(newHashedToken)
                .expiresAt(Instant.now().plusSeconds(refreshExpiration / 1000))
                .revoked(false)
                .build();
        refreshTokenRepository.save(newRefreshToken);

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", newAccessToken);
        tokens.put("refreshToken", newRefreshTokenString);
        return tokens;
    }

    @Transactional
    public void logout(LogoutRequest request) {
        String hashedToken = hashToken(request.getRefreshToken());
        RefreshToken token = refreshTokenRepository.findByToken(hashedToken)
                .orElseThrow(() -> new RefreshTokenInvalidException("Invalid refresh token"));

        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }

    @Transactional
    public Map<String, String> forgotPassword(ForgotPasswordRequest request) {
        // Invalidate active PASSWORD_RESET OTPs first
        invalidatePreviousOtps(request.getEmail(), VerificationPurpose.PASSWORD_RESET);

        if (!userRepository.existsByEmail(request.getEmail())) {
            // Log internally but return success response (empty map) to prevent user enumeration
            log.info("Forgot password requested for non-existent email: {}", request.getEmail());
            return Collections.emptyMap();
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

        // Invalidate active OTPs for the requested purpose
        invalidatePreviousOtps(request.getEmail(), request.getPurpose());

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

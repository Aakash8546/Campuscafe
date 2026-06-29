package com.campuscafe.backend.auth.controller;

import com.campuscafe.backend.auth.dto.*;
import com.campuscafe.backend.auth.service.AuthService;
import com.campuscafe.backend.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for merchant onboarding, login, logout, and password recovery")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @Operation(summary = "Onboard a new merchant and generate email verification OTP")
    public ResponseEntity<ApiResponse<Map<String, String>>> signup(@Valid @RequestBody SignupRequest request) {
        Map<String, String> data = authService.signup(request);
        return ResponseEntity.ok(ApiResponse.success("Merchant signup successful. Please verify OTP.", data));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify merchant email using OTP")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        authService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.success("OTP verified successfully. Merchant account activated.", null));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user credentials and return JWT tokens")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse data = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", data));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Rotate expired access token using a valid refresh token")
    public ResponseEntity<ApiResponse<Map<String, String>>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        Map<String, String> data = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Access token refreshed successfully", data));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke user's refresh token and end active session")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Initiate password reset request and generate OTP")
    public ResponseEntity<ApiResponse<Map<String, String>>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        Map<String, String> data = authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset OTP generated. Please check your log/email.", data));
    }

    @PostMapping("/resend-otp")
    @Operation(summary = "Resend verification OTP via email")
    public ResponseEntity<ApiResponse<Map<String, String>>> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        Map<String, String> data = authService.resendOtp(request);
        return ResponseEntity.ok(ApiResponse.success("Verification OTP resent successfully.", data));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using OTP and set new password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successful. You can now login with your new password.", null));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Securely change password for authenticated user")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.campuscafe.backend.security.service.CustomUserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        authService.changePassword(userDetails.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }
}

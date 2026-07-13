package com.campuscafe.backend.mail.service;

/**
 * Service interface for sending emails (OTPs, notifications, status updates).
 */
public interface EmailService {
    /**
     * Sends an OTP verification email to the user.
     */
    void sendOtpEmail(String recipientEmail, String otp, String purposeDescription);

    /**
     * Sends registration details and action links to the Super Admin for approval.
     */
    void sendSuperAdminNotification(String superAdminEmail, String cafeName, String ownerName, String email, String phone, String address, String city, String pincode, Long merchantId, String token);

    /**
     * Sends the merchant status update (Approved/Rejected) email to the owner.
     */
    void sendMerchantApprovalStatusEmail(String recipientEmail, String cafeName, String statusDescription);
}

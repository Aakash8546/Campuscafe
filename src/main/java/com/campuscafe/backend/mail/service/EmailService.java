package com.campuscafe.backend.mail.service;

public interface EmailService {
    void sendOtpEmail(String recipientEmail, String otp, String purposeDescription);
    void sendSuperAdminNotification(String superAdminEmail, String cafeName, String ownerName, String email, String phone, String address, String city, String pincode, Long merchantId, String token);
    void sendMerchantApprovalStatusEmail(String recipientEmail, String cafeName, String statusDescription);
}

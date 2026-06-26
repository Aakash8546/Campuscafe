package com.campuscafe.backend.mail.service;

public interface EmailService {
    void sendOtpEmail(String recipientEmail, String otp, String purposeDescription);
}

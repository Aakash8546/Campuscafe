package com.campuscafe.backend.mail.service;

import com.campuscafe.backend.mail.config.EmailProperties;
import com.campuscafe.backend.mail.exception.EmailSendFailedException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;
    private final EmailProperties emailProperties;
    private final Environment environment;

    @Override
    public void sendOtpEmail(String recipientEmail, String otp, String purposeDescription) {
        // Validation: Verify recipient email format
        if (recipientEmail == null || !recipientEmail.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new EmailSendFailedException("Invalid recipient email address: " + recipientEmail);
        }

        String template = getEmailHtmlTemplate(otp);
        int maxAttempts = isDevProfile() ? 1 : (emailProperties.getMaxAttempts() != null ? emailProperties.getMaxAttempts() : 10);
        long delay = emailProperties.getInitialDelayMs() != null ? emailProperties.getInitialDelayMs() : 1000;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                if (isDevProfile()) {
                    log.info("[DEV] Attempting to send OTP email to {} (Attempt {}/{}). OTP: {}", recipientEmail, attempt, maxAttempts, otp);
                } else {
                    log.info("Attempting to send OTP email to {} (Attempt {}/{})", recipientEmail, attempt, maxAttempts);
                }

                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setFrom(emailProperties.getMailFrom(), emailProperties.getMailFromName());
                helper.setTo(recipientEmail);
                helper.setSubject("Campus Cafe OTP Verification");
                helper.setText(template, true);

                mailSender.send(message);

                log.info("Email sent successfully to {}", recipientEmail);
                return; // successfully sent
            } catch (Exception e) {
                log.warn("Email send failed on attempt {}/{} for {}. Error: {}", attempt, maxAttempts, recipientEmail, e.getMessage());
                if (attempt == maxAttempts) {
                    if (isDevProfile()) {
                        log.error("[DEV] Failed to send OTP email to {}, but bypassing exception for local development.", recipientEmail, e);
                        return;
                    }
                    throw new EmailSendFailedException("Unable to send verification email. Please try again later.", e);
                }
                try {
                    if (delay > 0) {
                        Thread.sleep(delay);
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new EmailSendFailedException("Email sending interrupted during retry delay", ie);
                }
                delay *= 2; // exponential backoff
            }
        }
    }

    private String getEmailHtmlTemplate(String otp) {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Campus Cafe OTP Verification</title>
            <style>
                body {
                    font-family: 'Helvetica Neue', Arial, sans-serif;
                    background-color: #ffffff;
                    margin: 0;
                    padding: 0;
                    -webkit-font-smoothing: antialiased;
                }
                .container {
                    max-width: 600px;
                    margin: 40px auto;
                    padding: 30px;
                    border: 1px solid #e0e0e0;
                    border-radius: 8px;
                    background-color: #ffffff;
                }
                .header {
                    font-size: 24px;
                    font-weight: bold;
                    color: #5D4037; /* Brown Accent Color */
                    margin-bottom: 20px;
                    border-bottom: 2px solid #5D4037;
                    padding-bottom: 10px;
                }
                .content {
                    font-size: 16px;
                    line-height: 1.6;
                    color: #333333;
                    margin-bottom: 30px;
                }
                .otp-box {
                    font-size: 32px;
                    font-weight: bold;
                    color: #5D4037;
                    background-color: #F5F5F5;
                    padding: 15px;
                    text-align: center;
                    border-radius: 4px;
                    letter-spacing: 5px;
                    margin: 20px 0;
                    border: 1px dashed #5D4037;
                }
                .footer {
                    font-size: 14px;
                    color: #777777;
                    border-top: 1px solid #e0e0e0;
                    padding-top: 20px;
                    margin-top: 30px;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    Campus Cafe
                </div>
                <div class="content">
                    <p>Hello,</p>
                    <p>Your One Time Password (OTP) is</p>
                    <div class="otp-box">%s</div>
                    <p>This OTP is valid for 5 minutes.</p>
                    <p>If you did not request this OTP, please ignore this email.</p>
                </div>
                <div class="footer">
                    <p>Regards,</p>
                    <p><strong>Campus Cafe Team</strong></p>
                </div>
            </div>
        </body>
        </html>
        """.formatted(otp);
    }    private boolean isDevProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("dev");
    }

    @Override
    public void sendSuperAdminNotification(String superAdminEmail, String cafeName, String ownerName, String email, String phone, String address, String city, String pincode, Long merchantId, String token) {
        String baseUrl = isDevProfile() ? "http://localhost:8081" : "https://campuscafe-5201.onrender.com";
        String approveUrl = baseUrl + "/auth/super-admin/merchants/verify?merchantId=" + merchantId + "&token=" + token + "&action=VERIFIED";
        String rejectUrl = baseUrl + "/auth/super-admin/merchants/verify?merchantId=" + merchantId + "&token=" + token + "&action=REJECTED";

        String template = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <title>New Merchant Request</title>
            <style>
                body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                .container { max-width: 600px; margin: 20px auto; padding: 20px; border: 1px solid #ddd; border-radius: 5px; }
                .header { font-size: 20px; font-weight: bold; color: #5D4037; border-bottom: 2px solid #5D4037; padding-bottom: 10px; margin-bottom: 20px; }
                .details { margin-bottom: 20px; }
                .details td { padding: 5px 0; }
                .actions { margin-top: 30px; text-align: center; }
                .btn { display: inline-block; padding: 10px 20px; text-decoration: none; color: white; border-radius: 4px; font-weight: bold; }
                .btn-approve { background-color: #4CAF50; }
                .btn-reject { background-color: #f44336; margin-left: 15px; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">New Merchant Registration Request</div>
                <div class="content">
                    <p>A new merchant has signed up and is pending verification. Please review the details below:</p>
                    <table class="details" width="100%%">
                        <tr><td width="30%%"><strong>Cafe Name:</strong></td><td>%s</td></tr>
                        <tr><td><strong>Owner Name:</strong></td><td>%s</td></tr>
                        <tr><td><strong>Email:</strong></td><td>%s</td></tr>
                        <tr><td><strong>Phone:</strong></td><td>%s</td></tr>
                        <tr><td><strong>Address:</strong></td><td>%s</td></tr>
                        <tr><td><strong>City:</strong></td><td>%s</td></tr>
                        <tr><td><strong>Pincode:</strong></td><td>%s</td></tr>
                    </table>
                    <div class="actions">
                        <a href="%s" class="btn btn-approve">Approve Account</a>
                        <a href="%s" class="btn btn-reject">Reject Account</a>
                    </div>
                </div>
            </div>
        </body>
        </html>
        """.formatted(cafeName, ownerName, email, phone, address, city, pincode, approveUrl, rejectUrl);

        sendHtmlEmail(superAdminEmail, "Campus Cafe - New Merchant Approval Request", template);
    }

    @Override
    public void sendMerchantApprovalStatusEmail(String recipientEmail, String cafeName, String statusDescription) {
        String template = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <title>Merchant Account Update</title>
            <style>
                body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                .container { max-width: 600px; margin: 20px auto; padding: 20px; border: 1px solid #ddd; border-radius: 5px; }
                .header { font-size: 20px; font-weight: bold; color: #5D4037; border-bottom: 2px solid #5D4037; padding-bottom: 10px; margin-bottom: 20px; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">Campus Cafe - Account Verification Update</div>
                <div class="content">
                    <p>Dear %s Owner,</p>
                    <p>We are writing to inform you that your merchant account registration request has been <strong>%s</strong>.</p>
                    <p>If you have any questions, please contact the administrator.</p>
                </div>
            </div>
        </body>
        </html>
        """.formatted(cafeName, statusDescription);

        sendHtmlEmail(recipientEmail, "Campus Cafe - Account Update", template);
    }

    private void sendHtmlEmail(String recipientEmail, String subject, String htmlContent) {
        int maxAttempts = isDevProfile() ? 1 : (emailProperties.getMaxAttempts() != null ? emailProperties.getMaxAttempts() : 10);
        long delay = emailProperties.getInitialDelayMs() != null ? emailProperties.getInitialDelayMs() : 1000;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setFrom(emailProperties.getMailFrom(), emailProperties.getMailFromName());
                helper.setTo(recipientEmail);
                helper.setSubject(subject);
                helper.setText(htmlContent, true);

                mailSender.send(message);
                log.info("Notification email sent successfully to {}", recipientEmail);
                return;
            } catch (Exception e) {
                log.warn("Email send failed on attempt {}/{} for {}. Error: {}", attempt, maxAttempts, recipientEmail, e.getMessage());
                if (attempt == maxAttempts) {
                    if (isDevProfile()) {
                        log.error("[DEV] Failed to send HTML email to {}, but bypassing exception for local development.", recipientEmail, e);
                        return;
                    }
                    throw new EmailSendFailedException("Unable to send notification email. Please try again later.", e);
                }
                try {
                    if (delay > 0) {
                        Thread.sleep(delay);
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new EmailSendFailedException("Email sending interrupted during retry delay", ie);
                }
                delay *= 2;
            }
        }
    }
}

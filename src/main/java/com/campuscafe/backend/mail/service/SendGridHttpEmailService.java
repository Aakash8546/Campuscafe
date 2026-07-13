package com.campuscafe.backend.mail.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Service
@Primary
@Slf4j
public class SendGridHttpEmailService implements EmailService {

    private final ObjectMapper objectMapper;
    private final Environment environment;

    @Value("${mail.smtp.password:}")
    private String smtpPassword;

    @Value("${mail.smtp.from:no-reply@campuscafe.com}")
    private String mailFrom;

    @Value("${mail.smtp.from-name:Campus Cafe}")
    private String mailFromName;

    @Value("${mail.smtp.otp-expiry-minutes:5}")
    private int otpExpiryMinutes;

    public SendGridHttpEmailService(ObjectMapper objectMapper, Environment environment) {
        this.objectMapper = objectMapper;
        this.environment = environment;
    }

    private boolean isDevProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("dev");
    }

    private String getApiKey() {
        String envKey = System.getenv("SENDGRID_API_KEY");
        if (envKey != null && !envKey.trim().isEmpty()) {
            return envKey;
        }
        return smtpPassword;
    }

    private void sendViaHttp(String recipientEmail, String subject, String htmlContent) {
        String apiKey = getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.equals("dummy")) {
            if (isDevProfile()) {
                log.warn("[DEV] SendGrid API Key not configured or dummy, bypassing HTTP email dispatch to {}", recipientEmail);
                return;
            }
            throw new RuntimeException("SendGrid API Key is not configured.");
        }

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            // Construct SendGrid v3 Mail Send API JSON structure
            Map<String, Object> payload = new HashMap<>();
            
            Map<String, Object> personalization = new HashMap<>();
            personalization.put("to", Collections.singletonList(Collections.singletonMap("email", recipientEmail)));
            payload.put("personalizations", Collections.singletonList(personalization));

            Map<String, Object> fromMap = new HashMap<>();
            fromMap.put("email", mailFrom);
            fromMap.put("name", mailFromName);
            payload.put("from", fromMap);

            payload.put("subject", subject);

            Map<String, Object> contentMap = new HashMap<>();
            contentMap.put("type", "text/html");
            contentMap.put("value", htmlContent);
            payload.put("content", Collections.singletonList(contentMap));

            String jsonBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.sendgrid.com/v3/mail/send"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Email sent successfully via SendGrid HTTP API to {}", recipientEmail);
            } else {
                log.error("Failed to send email via SendGrid HTTP API. Status: {}, Response: {}", response.statusCode(), response.body());
                if (!isDevProfile()) {
                    throw new RuntimeException("Failed to send email via SendGrid: " + response.body());
                }
            }
        } catch (Exception e) {
            log.error("Error occurred while sending email via SendGrid HTTP API to {}", recipientEmail, e);
            if (!isDevProfile()) {
                throw new RuntimeException("Error occurred while sending email: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void sendOtpEmail(String recipientEmail, String otp, String purposeDescription) {
        String template = getEmailHtmlTemplate(otp);
        if (isDevProfile()) {
            log.info("[DEV] OTP Email payload for {} -> OTP: {}", recipientEmail, otp);
        }
        sendViaHttp(recipientEmail, "Campus Cafe OTP Verification", template);
    }

    @Override
    public void sendSuperAdminNotification(String superAdminEmail, String cafeName, String ownerName, String email, String phone, String address, String city, String pincode, Long merchantId, String token) {
        String baseUrl = isDevProfile() ? "http://localhost:8081" : "https://campuscafe-5201.onrender.com";
        String approveUrl = baseUrl + "/auth/super-admin/merchants/verify?merchantId=" + merchantId + "&token=" + token + "&action=VERIFIED";
        String rejectUrl = baseUrl + "/auth/super-admin/merchants/verify?merchantId=" + merchantId + "&token=" + token + "&action=REJECTED";

        if (isDevProfile()) {
            log.info("[DEV] Super Admin Action Links:\nApprove Link: {}\nReject Link: {}", approveUrl, rejectUrl);
        }

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

        sendViaHttp(superAdminEmail, "Campus Cafe - New Merchant Approval Request", template);
    }

    @Override
    public void sendMerchantApprovalStatusEmail(String recipientEmail, String cafeName, String statusDescription) {
        String template = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Campus Cafe - Onboarding Update</title>
            <style>
                body { font-family: 'Helvetica Neue', Arial, sans-serif; background-color: #ffffff; margin: 0; padding: 0; }
                .container { max-width: 600px; margin: 40px auto; padding: 30px; border: 1px solid #e0e0e0; border-radius: 8px; }
                .header { font-size: 24px; font-weight: bold; color: #5D4037; margin-bottom: 20px; border-bottom: 2px solid #5D4037; padding-bottom: 10px; }
                .content { font-size: 16px; line-height: 1.6; color: #333333; }
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

        sendViaHttp(recipientEmail, "Campus Cafe - Account Update", template);
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
                body { font-family: 'Helvetica Neue', Arial, sans-serif; background-color: #ffffff; margin: 0; padding: 0; }
                .container { max-width: 600px; margin: 40px auto; padding: 30px; border: 1px solid #e0e0e0; border-radius: 8px; }
                .header { font-size: 24px; font-weight: bold; color: #5D4037; margin-bottom: 20px; border-bottom: 2px solid #5D4037; padding-bottom: 10px; }
                .content { font-size: 16px; line-height: 1.6; color: #333333; }
                .otp-box { font-size: 32px; font-weight: bold; color: #5D4037; background-color: #F5F5F5; padding: 15px; text-align: center; border-radius: 4px; letter-spacing: 5px; margin: 20px 0; border: 1px dashed #5D4037; }
                .footer { font-size: 14px; color: #777777; border-top: 1px solid #e0e0e0; padding-top: 20px; margin-top: 30px; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">Campus Cafe</div>
                <div class="content">
                    <p>Hello,</p>
                    <p>Your One Time Password (OTP) is</p>
                    <div class="otp-box">%s</div>
                    <p>This OTP is valid for 5 minutes.</p>
                </div>
                <div class="footer">
                    <p>Regards,</p>
                    <p><strong>Campus Cafe Team</strong></p>
                </div>
            </div>
        </body>
        </html>
        """.formatted(otp);
    }
}

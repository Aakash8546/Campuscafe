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
        int maxAttempts = emailProperties.getMaxAttempts() != null ? emailProperties.getMaxAttempts() : 10;
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
    }

    private boolean isDevProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("dev");
    }
}

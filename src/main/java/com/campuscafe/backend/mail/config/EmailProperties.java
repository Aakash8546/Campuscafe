package com.campuscafe.backend.mail.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Data
public class EmailProperties {

    @Value("${mail.brevo.username}")
    private String username;

    @Value("${mail.brevo.password}")
    private String password;

    @Value("${mail.brevo.from}")
    private String mailFrom;

    @Value("${mail.brevo.from-name}")
    private String mailFromName;

    @Value("${mail.brevo.otp-expiry-minutes}")
    private Integer otpExpiryMinutes;

    @Value("${mail.brevo.max-attempts:10}")
    private Integer maxAttempts;

    @Value("${mail.brevo.initial-delay-ms:1000}")
    private Long initialDelayMs;
}

package com.campuscafe.backend.mail.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Data
public class EmailProperties {

    @Value("${mail.smtp.username:mock-username}")
    private String username;

    @Value("${mail.smtp.password:mock-password}")
    private String password;

    @Value("${mail.smtp.from:aakashsrivastava2151@gmail.com}")
    private String mailFrom;

    @Value("${mail.smtp.from-name:Aakash Srivastava}")
    private String mailFromName;

    @Value("${mail.smtp.otp-expiry-minutes:5}")
    private Integer otpExpiryMinutes;

    @Value("${mail.smtp.max-attempts:10}")
    private Integer maxAttempts;

    @Value("${mail.smtp.initial-delay-ms:1000}")
    private Long initialDelayMs;

    @Value("${mail.smtp.host:smtp.sendgrid.net}")
    private String host;

    @Value("${mail.smtp.port:587}")
    private Integer port;

    @Value("${mail.smtp.debug:false}")
    private Boolean debug;
}

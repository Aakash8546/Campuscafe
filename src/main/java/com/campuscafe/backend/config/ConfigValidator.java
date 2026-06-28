package com.campuscafe.backend.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConfigValidator {

    private final Environment environment;

    @Value("${application.security.jwt.secret-key}")
    private String jwtSecretKey;

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:}")
    private String datasourceUsername;

    @Value("${spring.datasource.password:}")
    private String datasourcePassword;

    @PostConstruct
    public void validateConfiguration() {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length == 0) {
            activeProfiles = environment.getDefaultProfiles();
        }
        log.info("==================================================================");
        log.info("SPRING ACTIVE PROFILES: {}", Arrays.toString(activeProfiles));
        log.info("==================================================================");

        boolean isProd = Arrays.asList(activeProfiles).contains("prod");

        if (jwtSecretKey == null || jwtSecretKey.trim().length() < 32) {
            throw new IllegalStateException("CONFIGURATION ERROR: JWT Secret Key must be at least 32 characters (256 bits) long.");
        }

        if (isProd) {
            log.info("Performing Production Environment Safety Validations...");

            if (datasourceUrl == null || datasourceUrl.isBlank()) {
                throw new IllegalStateException("PROD CONFIGURATION ERROR: Mandatory environment variable SPRING_DATASOURCE_URL is missing!");
            }
            if (datasourceUsername == null || datasourceUsername.isBlank()) {
                throw new IllegalStateException("PROD CONFIGURATION ERROR: Mandatory environment variable SPRING_DATASOURCE_USERNAME is missing!");
            }
            if (datasourcePassword == null || datasourcePassword.isBlank()) {
                throw new IllegalStateException("PROD CONFIGURATION ERROR: Mandatory environment variable SPRING_DATASOURCE_PASSWORD is missing!");
            }
            if ("9a6747fc6259aa374ab4e1bb2a0918dd2199978e5f19e6eef5a070514e21a2f9".equals(jwtSecretKey)) {
                throw new IllegalStateException("PROD CONFIGURATION ERROR: Default development JWT secret key cannot be used in PRODUCTION!");
            }
            log.info("Production Environment Configuration Validation Passed Successfully.");
        }
    }
}

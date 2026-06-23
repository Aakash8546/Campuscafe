package com.campuscafe.backend.security.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // Inject values manually for unit testing
        ReflectionTestUtils.setField(jwtService, "secretKey", "9a6747fc6259aa374ab4e1bb2a0918dd2199978e5f19e6eef5a070514e21a2f9");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 900000L); // 15 mins
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", 604800000L); // 7 days

        userDetails = new User("test@campuscafe.com", "Password@123", Collections.emptyList());
    }

    @Test
    void testGenerateAccessToken_ValidToken() {
        String token = jwtService.generateAccessToken(userDetails);
        assertNotNull(token);
        assertEquals("test@campuscafe.com", jwtService.extractUsername(token));
        assertTrue(jwtService.validateToken(token, userDetails));
    }

    @Test
    void testGenerateRefreshToken_ValidToken() {
        String token = jwtService.generateRefreshToken(userDetails);
        assertNotNull(token);
        assertEquals("test@campuscafe.com", jwtService.extractUsername(token));
        assertTrue(jwtService.validateToken(token, userDetails));
    }
}

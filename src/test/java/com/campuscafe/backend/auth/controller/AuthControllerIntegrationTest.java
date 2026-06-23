package com.campuscafe.backend.auth.controller;

import com.campuscafe.backend.auth.dto.LoginRequest;
import com.campuscafe.backend.auth.dto.LoginResponse;
import com.campuscafe.backend.auth.dto.SignupRequest;
import com.campuscafe.backend.auth.service.AuthService;
import com.campuscafe.backend.security.service.CustomUserDetailsService;
import com.campuscafe.backend.security.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters to simplify controller-only testing
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    void testSignup_Success() throws Exception {
        SignupRequest request = SignupRequest.builder()
                .cafeName("Test Cafe")
                .email("admin@test.com")
                .phone("9876543210")
                .password("Password@123")
                .build();

        when(authService.signup(any(SignupRequest.class))).thenReturn(Collections.singletonMap("otp", "123456"));

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.otp").value("123456"));
    }

    @Test
    void testLogin_Success() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("admin@test.com")
                .password("Password@123")
                .build();

        LoginResponse response = LoginResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .role("ADMIN")
                .merchantId(1L)
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }
}

package com.campuscafe.backend.user.controller;

import com.campuscafe.backend.domain.merchant.Merchant;
import com.campuscafe.backend.domain.user.Permission;
import com.campuscafe.backend.domain.user.Role;
import com.campuscafe.backend.domain.user.User;
import com.campuscafe.backend.exception.AccessDeniedException;
import com.campuscafe.backend.security.service.CustomUserDetails;
import com.campuscafe.backend.security.service.CustomUserDetailsService;
import com.campuscafe.backend.security.service.JwtService;
import com.campuscafe.backend.user.dto.CreateUserRequest;
import com.campuscafe.backend.user.dto.UpdateUserRequest;
import com.campuscafe.backend.user.dto.UserResponse;
import com.campuscafe.backend.user.dto.UserSummaryResponse;
import com.campuscafe.backend.user.service.UserService;
import com.campuscafe.backend.auth.service.UserLoginLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campuscafe.backend.security.config.SecurityConfig;
import com.campuscafe.backend.security.filter.JwtAuthenticationFilter;
import org.springframework.context.annotation.Import;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private UserLoginLogService userLoginLogService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    private CustomUserDetails adminPrincipal;
    private CustomUserDetails managerPrincipal;
    private CustomUserDetails cashierPrincipal;

    @BeforeEach
    void setUp() {
        Merchant merchant = Merchant.builder().email("merchant@test.com").build();
        merchant.setId(1L);

        Permission createPerm = Permission.builder().name("USER_CREATE").build();
        Permission viewPerm = Permission.builder().name("USER_VIEW").build();
        Permission updatePerm = Permission.builder().name("USER_UPDATE").build();
        Permission deletePerm = Permission.builder().name("USER_DELETE").build();

        Role adminRole = Role.builder().name("ADMIN").permissions(Set.of(createPerm, viewPerm, updatePerm, deletePerm)).build();
        User admin = User.builder().email("admin@test.com").role(adminRole).merchant(merchant).active(true).build();
        admin.setId(1L);
        adminPrincipal = new CustomUserDetails(admin);

        Role managerRole = Role.builder().name("MANAGER").permissions(Set.of(viewPerm)).build();
        User manager = User.builder().email("manager@test.com").role(managerRole).merchant(merchant).active(true).build();
        manager.setId(2L);
        managerPrincipal = new CustomUserDetails(manager);

        Role cashierRole = Role.builder().name("CASHIER").permissions(Collections.emptySet()).build();
        User cashier = User.builder().email("cashier@test.com").role(cashierRole).merchant(merchant).active(true).build();
        cashier.setId(3L);
        cashierPrincipal = new CustomUserDetails(cashier);
    }

    @Test
    void testCreateUser_AsAdmin_Success() throws Exception {
        CreateUserRequest request = CreateUserRequest.builder()
                .name("New cashier")
                .email("new@test.com")
                .phone("123456")
                .password("Password@123")
                .role("CASHIER")
                .build();

        UserResponse response = UserResponse.builder()
                .id(4L)
                .name("New cashier")
                .email("new@test.com")
                .phone("123456")
                .role("CASHIER")
                .active(true)
                .build();

        when(userService.createUser(any(CreateUserRequest.class))).thenReturn(response);

        mockMvc.perform(post("/users")
                        .with(csrf())
                        .with(user(adminPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(4))
                .andExpect(jsonPath("$.data.name").value("New cashier"));

        verify(userService, times(1)).createUser(any(CreateUserRequest.class));
    }

    @Test
    void testCreateUser_AsManager_Forbidden() throws Exception {
        CreateUserRequest request = CreateUserRequest.builder()
                .name("New cashier")
                .email("new@test.com")
                .phone("123456")
                .password("Password@123")
                .role("CASHIER")
                .build();

        mockMvc.perform(post("/users")
                        .with(csrf())
                        .with(user(managerPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        verify(userService, never()).createUser(any(CreateUserRequest.class));
    }

    @Test
    void testGetAllUsers_AsManager_Success() throws Exception {
        UserSummaryResponse summary = UserSummaryResponse.builder()
                .id(2L)
                .name("Manager")
                .email("manager@test.com")
                .role("MANAGER")
                .active(true)
                .build();

        when(userService.getAllUsers()).thenReturn(List.of(summary));

        mockMvc.perform(get("/users")
                        .with(user(managerPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].email").value("manager@test.com"));

        verify(userService, times(1)).getAllUsers();
    }

    @Test
    void testGetAllUsers_AsCashier_Forbidden() throws Exception {
        mockMvc.perform(get("/users")
                        .with(user(cashierPrincipal)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        verify(userService, never()).getAllUsers();
    }

    @Test
    void testGetUserById_Success() throws Exception {
        UserResponse response = UserResponse.builder()
                .id(2L)
                .name("Manager")
                .email("manager@test.com")
                .role("MANAGER")
                .active(true)
                .build();

        when(userService.getUserById(2L)).thenReturn(response);

        mockMvc.perform(get("/users/2")
                        .with(user(managerPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("manager@test.com"));
    }

    @Test
    void testGetUserById_CrossTenant_Forbidden() throws Exception {
        when(userService.getUserById(anyLong())).thenThrow(new AccessDeniedException("Access denied"));

        mockMvc.perform(get("/users/10")
                        .with(user(adminPrincipal)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void testGetLoginLogs_Success() throws Exception {
        com.campuscafe.backend.auth.dto.UserLoginLogResponse logDto = com.campuscafe.backend.auth.dto.UserLoginLogResponse.builder()
                .id(1L)
                .email("admin@test.com")
                .status(com.campuscafe.backend.domain.user.LoginStatus.SUCCESS)
                .build();

        org.springframework.data.domain.Page<com.campuscafe.backend.auth.dto.UserLoginLogResponse> page = new org.springframework.data.domain.PageImpl<>(List.of(logDto));
        when(userLoginLogService.getLoginLogs(anyLong(), any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/users/login-logs")
                        .with(user(adminPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].email").value("admin@test.com"));
    }
}

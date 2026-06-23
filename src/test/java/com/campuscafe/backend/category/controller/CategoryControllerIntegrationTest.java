package com.campuscafe.backend.category.controller;

import com.campuscafe.backend.category.dto.CategoryResponse;
import com.campuscafe.backend.category.dto.CreateCategoryRequest;
import com.campuscafe.backend.category.dto.UpdateCategoryRequest;
import com.campuscafe.backend.category.service.CategoryService;
import com.campuscafe.backend.common.ApiResponse;
import com.campuscafe.backend.domain.merchant.Merchant;
import com.campuscafe.backend.domain.user.Permission;
import com.campuscafe.backend.domain.user.Role;
import com.campuscafe.backend.domain.user.User;
import com.campuscafe.backend.security.config.SecurityConfig;
import com.campuscafe.backend.security.filter.JwtAuthenticationFilter;
import com.campuscafe.backend.security.service.CustomUserDetails;
import com.campuscafe.backend.security.service.CustomUserDetailsService;
import com.campuscafe.backend.security.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class CategoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

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

        Permission createPerm = Permission.builder().name("CATEGORY_CREATE").build();
        Permission viewPerm = Permission.builder().name("CATEGORY_VIEW").build();
        Permission updatePerm = Permission.builder().name("CATEGORY_UPDATE").build();
        Permission deletePerm = Permission.builder().name("CATEGORY_DELETE").build();

        Role adminRole = Role.builder().name("ADMIN").permissions(Set.of(createPerm, viewPerm, updatePerm, deletePerm)).build();
        User admin = User.builder().email("admin@test.com").role(adminRole).merchant(merchant).active(true).build();
        admin.setId(1L);
        adminPrincipal = new CustomUserDetails(admin);

        Role managerRole = Role.builder().name("MANAGER").permissions(Set.of(createPerm, viewPerm, updatePerm)).build();
        User manager = User.builder().email("manager@test.com").role(managerRole).merchant(merchant).active(true).build();
        manager.setId(2L);
        managerPrincipal = new CustomUserDetails(manager);

        Role cashierRole = Role.builder().name("CASHIER").permissions(Set.of(viewPerm)).build();
        User cashier = User.builder().email("cashier@test.com").role(cashierRole).merchant(merchant).active(true).build();
        cashier.setId(3L);
        cashierPrincipal = new CustomUserDetails(cashier);
    }

    @Test
    void testCreateCategory_AsAdmin_Success() throws Exception {
        CreateCategoryRequest request = CreateCategoryRequest.builder().name("Snacks").build();
        CategoryResponse response = CategoryResponse.builder().id(5L).name("Snacks").active(true).build();

        when(categoryService.createCategory(any(CreateCategoryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/categories")
                        .with(csrf())
                        .with(user(adminPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Snacks"));

        verify(categoryService, times(1)).createCategory(any(CreateCategoryRequest.class));
    }

    @Test
    void testCreateCategory_AsCashier_Forbidden() throws Exception {
        CreateCategoryRequest request = CreateCategoryRequest.builder().name("Snacks").build();

        mockMvc.perform(post("/categories")
                        .with(csrf())
                        .with(user(cashierPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(categoryService, never()).createCategory(any(CreateCategoryRequest.class));
    }

    @Test
    void testGetAllCategories_AsCashier_Success() throws Exception {
        CategoryResponse response = CategoryResponse.builder().id(5L).name("Snacks").active(true).build();

        when(categoryService.getAllCategories()).thenReturn(List.of(response));

        mockMvc.perform(get("/categories")
                        .with(user(cashierPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Snacks"));
    }
}

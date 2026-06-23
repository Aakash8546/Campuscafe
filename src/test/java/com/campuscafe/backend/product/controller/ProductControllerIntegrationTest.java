package com.campuscafe.backend.product.controller;

import com.campuscafe.backend.common.ApiResponse;
import com.campuscafe.backend.domain.merchant.Merchant;
import com.campuscafe.backend.domain.user.Permission;
import com.campuscafe.backend.domain.user.Role;
import com.campuscafe.backend.domain.user.User;
import com.campuscafe.backend.product.dto.*;
import com.campuscafe.backend.product.service.ProductService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

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

        Permission createPerm = Permission.builder().name("PRODUCT_CREATE").build();
        Permission viewPerm = Permission.builder().name("PRODUCT_VIEW").build();
        Permission updatePerm = Permission.builder().name("PRODUCT_UPDATE").build();
        Permission deletePerm = Permission.builder().name("PRODUCT_DELETE").build();

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
    void testCreateProduct_AsAdmin_Success() throws Exception {
        CreateProductRequest request = CreateProductRequest.builder()
                .name("Cold Brew")
                .price(new BigDecimal("5.00"))
                .categoryId(1L)
                .build();
        ProductResponse response = ProductResponse.builder().id(10L).name("Cold Brew").price(new BigDecimal("5.00")).available(true).build();

        when(productService.createProduct(any(CreateProductRequest.class))).thenReturn(response);

        mockMvc.perform(post("/products")
                        .with(csrf())
                        .with(user(adminPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Cold Brew"));

        verify(productService, times(1)).createProduct(any(CreateProductRequest.class));
    }

    @Test
    void testCreateProduct_AsCashier_Forbidden() throws Exception {
        CreateProductRequest request = CreateProductRequest.builder()
                .name("Cold Brew")
                .price(new BigDecimal("5.00"))
                .categoryId(1L)
                .build();

        mockMvc.perform(post("/products")
                        .with(csrf())
                        .with(user(cashierPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(productService, never()).createProduct(any(CreateProductRequest.class));
    }

    @Test
    void testSearchProducts_Success() throws Exception {
        ProductSummaryResponse summary = ProductSummaryResponse.builder().id(10L).name("Cold Brew").price(new BigDecimal("5.00")).available(true).build();
        Page<ProductSummaryResponse> page = new PageImpl<>(List.of(summary));

        when(productService.searchProducts(any(), any(), any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/products")
                        .with(user(cashierPrincipal))
                        .param("name", "coffee")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].name").value("Cold Brew"));
    }

    @Test
    void testToggleAvailability_AsAdmin_Success() throws Exception {
        ProductResponse response = ProductResponse.builder().id(10L).name("Cold Brew").available(false).build();
        when(productService.toggleAvailability(10L)).thenReturn(response);

        mockMvc.perform(patch("/products/10/availability")
                        .with(csrf())
                        .with(user(adminPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.available").value(false));

        verify(productService, times(1)).toggleAvailability(10L);
    }

    @Test
    void testToggleAvailability_AsCashier_Forbidden() throws Exception {
        mockMvc.perform(patch("/products/10/availability")
                        .with(csrf())
                        .with(user(cashierPrincipal)))
                .andExpect(status().isForbidden());

        verify(productService, never()).toggleAvailability(anyLong());
    }
}

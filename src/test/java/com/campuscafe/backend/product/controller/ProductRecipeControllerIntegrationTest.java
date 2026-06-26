package com.campuscafe.backend.product.controller;

import com.campuscafe.backend.common.ApiResponse;
import com.campuscafe.backend.domain.merchant.Merchant;
import com.campuscafe.backend.domain.user.Permission;
import com.campuscafe.backend.domain.user.Role;
import com.campuscafe.backend.domain.user.User;
import com.campuscafe.backend.product.dto.RecipeRequest;
import com.campuscafe.backend.product.dto.RecipeResponse;
import com.campuscafe.backend.product.service.ProductRecipeService;
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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductRecipeController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ProductRecipeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductRecipeService productRecipeService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    private CustomUserDetails adminPrincipal;
    private CustomUserDetails managerPrincipal;
    private CustomUserDetails cashierPrincipal;

    @BeforeEach
    void setUp() {
        Merchant merchant = Merchant.builder().email("owner@campuscafe.com").verified(true).active(true).build();
        merchant.setId(1L);

        // Permissions Set
        Set<Permission> adminPerms = Set.of(
                createPermission("RECIPE_CREATE"),
                createPermission("RECIPE_VIEW"),
                createPermission("RECIPE_UPDATE"),
                createPermission("RECIPE_DELETE")
        );
        Role adminRole = Role.builder().name("ADMIN").permissions(adminPerms).build();
        User admin = User.builder().merchant(merchant).name("Admin").email("owner@campuscafe.com").role(adminRole).active(true).build();
        admin.setId(1L);
        adminPrincipal = new CustomUserDetails(admin);

        Set<Permission> managerPerms = Set.of(
                createPermission("RECIPE_CREATE"),
                createPermission("RECIPE_VIEW"),
                createPermission("RECIPE_UPDATE"),
                createPermission("RECIPE_DELETE")
        );
        Role managerRole = Role.builder().name("MANAGER").permissions(managerPerms).build();
        User manager = User.builder().merchant(merchant).name("Manager").email("mgr@campuscafe.com").role(managerRole).active(true).build();
        manager.setId(2L);
        managerPrincipal = new CustomUserDetails(manager);

        Role cashierRole = Role.builder().name("CASHIER").permissions(Collections.emptySet()).build();
        User cashier = User.builder().merchant(merchant).name("Cashier").email("csh@campuscafe.com").role(cashierRole).active(true).build();
        cashier.setId(3L);
        cashierPrincipal = new CustomUserDetails(cashier);
    }

    private Permission createPermission(String name) {
        Permission p = new Permission();
        p.setName(name);
        return p;
    }

    @Test
    void testAddRecipeItem_AsAdmin_Success() throws Exception {
        RecipeRequest request = RecipeRequest.builder()
                .inventoryItemId(20L)
                .quantityRequired(BigDecimal.ONE)
                .build();

        RecipeResponse response = RecipeResponse.builder()
                .id(100L)
                .productId(10L)
                .inventoryItemId(20L)
                .quantityRequired(BigDecimal.ONE)
                .build();

        when(productRecipeService.addRecipeItem(eq(10L), any(RecipeRequest.class))).thenReturn(response);

        mockMvc.perform(post("/products/10/recipes")
                        .with(csrf())
                        .with(user(adminPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(100L));
    }

    @Test
    void testAddRecipeItem_AsCashier_Forbidden() throws Exception {
        RecipeRequest request = RecipeRequest.builder()
                .inventoryItemId(20L)
                .quantityRequired(BigDecimal.ONE)
                .build();

        mockMvc.perform(post("/products/10/recipes")
                        .with(csrf())
                        .with(user(cashierPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetRecipe_AsManager_Success() throws Exception {
        when(productRecipeService.getRecipe(10L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/products/10/recipes")
                        .with(user(managerPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void testUpdateRecipe_AsAdmin_Success() throws Exception {
        RecipeRequest request = RecipeRequest.builder()
                .inventoryItemId(20L)
                .quantityRequired(BigDecimal.TEN)
                .build();

        RecipeResponse response = RecipeResponse.builder()
                .id(100L)
                .productId(10L)
                .inventoryItemId(20L)
                .quantityRequired(BigDecimal.TEN)
                .build();

        when(productRecipeService.updateRecipe(eq(10L), eq(100L), any(RecipeRequest.class))).thenReturn(response);

        mockMvc.perform(put("/products/10/recipes/100")
                        .with(csrf())
                        .with(user(adminPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testDeleteRecipe_AsAdmin_Success() throws Exception {
        doNothing().when(productRecipeService).deleteRecipe(10L, 100L);

        mockMvc.perform(delete("/products/10/recipes/100")
                        .with(csrf())
                        .with(user(adminPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}

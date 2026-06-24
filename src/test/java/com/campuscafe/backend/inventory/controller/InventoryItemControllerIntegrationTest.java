package com.campuscafe.backend.inventory.controller;

import com.campuscafe.backend.common.ApiResponse;
import com.campuscafe.backend.domain.merchant.Merchant;
import com.campuscafe.backend.domain.user.Permission;
import com.campuscafe.backend.domain.user.Role;
import com.campuscafe.backend.domain.user.User;
import com.campuscafe.backend.inventory.dto.*;
import com.campuscafe.backend.inventory.service.InventoryItemService;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InventoryItemController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class InventoryItemControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InventoryItemService itemService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    private CustomUserDetails adminPrincipal;
    private CustomUserDetails managerPrincipal;
    private CustomUserDetails cashierPrincipal;
    private CustomUserDetails guestPrincipal;

    @BeforeEach
    void setUp() {
        Merchant merchant = Merchant.builder().email("merchant@test.com").build();
        merchant.setId(1L);

        Permission createPerm = Permission.builder().name("INVENTORY_CREATE").build();
        Permission viewPerm = Permission.builder().name("INVENTORY_VIEW").build();
        Permission updatePerm = Permission.builder().name("INVENTORY_UPDATE").build();
        Permission deletePerm = Permission.builder().name("INVENTORY_DELETE").build();

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

        Role guestRole = Role.builder().name("GUEST").permissions(Collections.emptySet()).build();
        User guest = User.builder().email("guest@test.com").role(guestRole).merchant(merchant).active(true).build();
        guest.setId(4L);
        guestPrincipal = new CustomUserDetails(guest);
    }

    @Test
    void testCreateItem_AsAdmin_Success() throws Exception {
        CreateInventoryItemRequest request = CreateInventoryItemRequest.builder()
                .name("Sugar")
                .unit("KG")
                .currentStock(new BigDecimal("5.000"))
                .minStock(new BigDecimal("1.000"))
                .maxStock(new BigDecimal("10.000"))
                .categoryId(1L)
                .build();

        InventoryItemResponse response = InventoryItemResponse.builder()
                .id(10L)
                .name("Sugar")
                .unit("KG")
                .currentStock(new BigDecimal("5.000"))
                .build();

        when(itemService.createItem(any(CreateInventoryItemRequest.class))).thenReturn(response);

        mockMvc.perform(post("/inventory/items")
                        .with(csrf())
                        .with(user(adminPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Sugar"));

        verify(itemService, times(1)).createItem(any(CreateInventoryItemRequest.class));
    }

    @Test
    void testCreateItem_AsGuest_Forbidden() throws Exception {
        CreateInventoryItemRequest request = CreateInventoryItemRequest.builder()
                .name("Sugar")
                .unit("KG")
                .currentStock(new BigDecimal("5.000"))
                .minStock(new BigDecimal("1.000"))
                .maxStock(new BigDecimal("10.000"))
                .categoryId(1L)
                .build();

        mockMvc.perform(post("/inventory/items")
                        .with(csrf())
                        .with(user(guestPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetAllItems_AsCashier_Success() throws Exception {
        InventoryItemSummaryResponse summary = InventoryItemSummaryResponse.builder()
                .id(10L)
                .name("Sugar")
                .unit("KG")
                .currentStock(new BigDecimal("5.000"))
                .categoryName("Raw Material")
                .build();

        Page<InventoryItemSummaryResponse> page = new PageImpl<>(List.of(summary));

        when(itemService.getAllItems(eq("Sugar"), eq(null), eq(null), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/inventory/items")
                        .with(user(cashierPrincipal))
                        .param("name", "Sugar")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].name").value("Sugar"));
    }

    @Test
    void testStockIn_AsManager_Success() throws Exception {
        StockInRequest request = StockInRequest.builder()
                .inventoryItemId(10L)
                .quantity(new BigDecimal("5.000"))
                .remarks("Incoming")
                .build();

        InventoryItemResponse response = InventoryItemResponse.builder()
                .id(10L)
                .currentStock(new BigDecimal("10.000"))
                .build();

        when(itemService.stockIn(any(StockInRequest.class))).thenReturn(response);

        mockMvc.perform(post("/inventory/stock-in")
                        .with(csrf())
                        .with(user(managerPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.currentStock").value(10));
    }

    @Test
    void testStockOut_AsCashier_Forbidden() throws Exception {
        StockOutRequest request = StockOutRequest.builder()
                .inventoryItemId(10L)
                .quantity(new BigDecimal("2.000"))
                .build();

        mockMvc.perform(post("/inventory/stock-out")
                        .with(csrf())
                        .with(user(cashierPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetDashboard_AsCashier_Success() throws Exception {
        InventoryDashboardResponse response = InventoryDashboardResponse.builder()
                .totalItems(15L)
                .lowStockItems(3L)
                .totalCategories(4L)
                .build();

        when(itemService.getDashboardMetrics()).thenReturn(response);

        mockMvc.perform(get("/inventory/dashboard")
                        .with(user(cashierPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalItems").value(15))
                .andExpect(jsonPath("$.data.lowStockItems").value(3));
    }
}

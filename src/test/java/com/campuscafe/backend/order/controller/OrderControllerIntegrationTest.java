package com.campuscafe.backend.order.controller;

import com.campuscafe.backend.common.ApiResponse;
import com.campuscafe.backend.domain.merchant.Merchant;
import com.campuscafe.backend.domain.order.enums.OrderSource;
import com.campuscafe.backend.domain.order.enums.OrderStatus;
import com.campuscafe.backend.domain.user.Permission;
import com.campuscafe.backend.domain.user.Role;
import com.campuscafe.backend.domain.user.User;
import com.campuscafe.backend.order.dto.*;
import com.campuscafe.backend.order.service.OrderService;
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

@WebMvcTest(OrderController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

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

        Permission createPerm = Permission.builder().name("ORDER_CREATE").build();
        Permission viewPerm = Permission.builder().name("ORDER_VIEW").build();
        Permission updatePerm = Permission.builder().name("ORDER_UPDATE").build();

        Role adminRole = Role.builder().name("ADMIN").permissions(Set.of(createPerm, viewPerm, updatePerm)).build();
        User admin = User.builder().email("admin@test.com").role(adminRole).merchant(merchant).active(true).build();
        admin.setId(1L);
        adminPrincipal = new CustomUserDetails(admin);

        Role managerRole = Role.builder().name("MANAGER").permissions(Set.of(createPerm, viewPerm, updatePerm)).build();
        User manager = User.builder().email("manager@test.com").role(managerRole).merchant(merchant).active(true).build();
        manager.setId(2L);
        managerPrincipal = new CustomUserDetails(manager);

        Role cashierRole = Role.builder().name("CASHIER").permissions(Set.of(createPerm, viewPerm, updatePerm)).build();
        User cashier = User.builder().email("cashier@test.com").role(cashierRole).merchant(merchant).active(true).build();
        cashier.setId(3L);
        cashierPrincipal = new CustomUserDetails(cashier);

        Role guestRole = Role.builder().name("GUEST").permissions(Collections.emptySet()).build();
        User guest = User.builder().email("guest@test.com").role(guestRole).merchant(merchant).active(true).build();
        guest.setId(4L);
        guestPrincipal = new CustomUserDetails(guest);
    }

    @Test
    void testCreateOrder_AsCashier_Success() throws Exception {
        OrderItemRequest itemReq = OrderItemRequest.builder().productId(10L).quantity(2).build();
        CreateOrderRequest request = CreateOrderRequest.builder()
                .source(OrderSource.OFFLINE)
                .items(List.of(itemReq))
                .build();

        OrderResponse response = OrderResponse.builder()
                .id(100L)
                .orderNumber("ORD-20260624-0001")
                .status("NEW")
                .finalAmount(new BigDecimal("9.00"))
                .build();

        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(response);

        mockMvc.perform(post("/orders")
                        .with(csrf())
                        .with(user(cashierPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderNumber").value("ORD-20260624-0001"));

        verify(orderService, times(1)).createOrder(any(CreateOrderRequest.class));
    }

    @Test
    void testCreateOrder_AsGuest_Forbidden() throws Exception {
        OrderItemRequest itemReq = OrderItemRequest.builder().productId(10L).quantity(2).build();
        CreateOrderRequest request = CreateOrderRequest.builder()
                .source(OrderSource.OFFLINE)
                .items(List.of(itemReq))
                .build();

        mockMvc.perform(post("/orders")
                        .with(csrf())
                        .with(user(guestPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetOrders_AsCashier_Success() throws Exception {
        OrderResponse response = OrderResponse.builder()
                .id(100L)
                .orderNumber("ORD-20260624-0001")
                .status("NEW")
                .build();

        Page<OrderResponse> page = new PageImpl<>(List.of(response));

        when(orderService.getOrders(eq(OrderStatus.NEW), eq(null), eq(null), eq(null), eq(null), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/orders")
                        .with(user(cashierPrincipal))
                        .param("status", "NEW")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].orderNumber").value("ORD-20260624-0001"));
    }

    @Test
    void testUpdateStatus_AsManager_Success() throws Exception {
        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder().status("PREPARING").build();
        OrderResponse response = OrderResponse.builder().id(100L).status("PREPARING").build();

        when(orderService.updateOrderStatus(eq(100L), any(UpdateOrderStatusRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/orders/100/status")
                        .with(csrf())
                        .with(user(managerPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PREPARING"));
    }

    @Test
    void testGetBoard_AsCashier_Success() throws Exception {
        OrderBoardResponse response = OrderBoardResponse.builder()
                .newOrders(Collections.emptyList())
                .preparing(Collections.emptyList())
                .ready(Collections.emptyList())
                .build();

        when(orderService.getOrderBoard()).thenReturn(response);

        mockMvc.perform(get("/orders/board")
                        .with(user(cashierPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}

package com.campuscafe.backend.integration;

import com.campuscafe.backend.discount.controller.DiscountController;
import com.campuscafe.backend.discount.dto.CreateDiscountRequest;
import com.campuscafe.backend.discount.dto.DiscountResponse;
import com.campuscafe.backend.discount.dto.UpdateDiscountRequest;
import com.campuscafe.backend.discount.service.DiscountService;
import com.campuscafe.backend.domain.discount.enums.DiscountType;
import com.campuscafe.backend.domain.merchant.Merchant;
import com.campuscafe.backend.domain.notification.enums.NotificationType;
import com.campuscafe.backend.domain.user.Permission;
import com.campuscafe.backend.domain.user.Role;
import com.campuscafe.backend.domain.user.User;
import com.campuscafe.backend.notification.controller.NotificationController;
import com.campuscafe.backend.notification.dto.NotificationResponse;
import com.campuscafe.backend.notification.dto.UnreadCountResponse;
import com.campuscafe.backend.notification.service.NotificationService;
import com.campuscafe.backend.security.config.SecurityConfig;
import com.campuscafe.backend.security.filter.JwtAuthenticationFilter;
import com.campuscafe.backend.security.service.CustomUserDetails;
import com.campuscafe.backend.security.service.CustomUserDetailsService;
import com.campuscafe.backend.security.service.JwtService;
import com.campuscafe.backend.settings.controller.MerchantSettingController;
import com.campuscafe.backend.settings.dto.MerchantSettingResponse;
import com.campuscafe.backend.settings.dto.UpdateMerchantSettingRequest;
import com.campuscafe.backend.settings.service.MerchantSettingService;
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

@WebMvcTest({DiscountController.class, NotificationController.class, MerchantSettingController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class Phase7ControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DiscountService discountService;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private MerchantSettingService merchantSettingService;

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

        Set<Permission> allPermissions = Set.of(
                "DISCOUNT_CREATE", "DISCOUNT_VIEW", "DISCOUNT_UPDATE", "DISCOUNT_DELETE",
                "NOTIFICATION_VIEW", "SETTINGS_VIEW", "SETTINGS_UPDATE"
        ).stream().map(name -> Permission.builder().name(name).build()).collect(Collectors.toSet());

        Set<Permission> managerPermissions = Set.of(
                "DISCOUNT_CREATE", "DISCOUNT_VIEW", "DISCOUNT_UPDATE",
                "NOTIFICATION_VIEW", "SETTINGS_VIEW", "SETTINGS_UPDATE"
        ).stream().map(name -> Permission.builder().name(name).build()).collect(Collectors.toSet());

        Set<Permission> cashierPermissions = Set.of(
                "DISCOUNT_VIEW", "NOTIFICATION_VIEW", "SETTINGS_VIEW"
        ).stream().map(name -> Permission.builder().name(name).build()).collect(Collectors.toSet());

        Role adminRole = Role.builder().name("ADMIN").permissions(allPermissions).build();
        User admin = User.builder().email("admin@test.com").role(adminRole).merchant(merchant).active(true).build();
        admin.setId(1L);
        adminPrincipal = new CustomUserDetails(admin);

        Role managerRole = Role.builder().name("MANAGER").permissions(managerPermissions).build();
        User manager = User.builder().email("manager@test.com").role(managerRole).merchant(merchant).active(true).build();
        manager.setId(2L);
        managerPrincipal = new CustomUserDetails(manager);

        Role cashierRole = Role.builder().name("CASHIER").permissions(cashierPermissions).build();
        User cashier = User.builder().email("cashier@test.com").role(cashierRole).merchant(merchant).active(true).build();
        cashier.setId(3L);
        cashierPrincipal = new CustomUserDetails(cashier);

        Role guestRole = Role.builder().name("GUEST").permissions(Collections.emptySet()).build();
        User guest = User.builder().email("guest@test.com").role(guestRole).merchant(merchant).active(true).build();
        guest.setId(4L);
        guestPrincipal = new CustomUserDetails(guest);
    }

    // ----------------------------------------------------
    // DISCOUNT ENDPOINT TESTS
    // ----------------------------------------------------

    @Test
    void testCreateDiscount_AsManager_Success() throws Exception {
        CreateDiscountRequest request = CreateDiscountRequest.builder()
                .name("NEWYEAR10")
                .discountType(DiscountType.PERCENTAGE)
                .value(new BigDecimal("10.00"))
                .active(true)
                .build();

        DiscountResponse response = DiscountResponse.builder()
                .id(1L)
                .name("NEWYEAR10")
                .discountType(DiscountType.PERCENTAGE)
                .value(new BigDecimal("10.00"))
                .active(true)
                .build();

        when(discountService.createDiscount(any(CreateDiscountRequest.class))).thenReturn(response);

        mockMvc.perform(post("/discounts")
                        .with(csrf())
                        .with(user(managerPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("NEWYEAR10"));
    }

    @Test
    void testCreateDiscount_AsCashier_Forbidden() throws Exception {
        CreateDiscountRequest request = CreateDiscountRequest.builder()
                .name("NEWYEAR10")
                .discountType(DiscountType.PERCENTAGE)
                .value(new BigDecimal("10.00"))
                .build();

        mockMvc.perform(post("/discounts")
                        .with(csrf())
                        .with(user(cashierPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetDiscounts_AsCashier_Success() throws Exception {
        DiscountResponse res = DiscountResponse.builder().id(1L).name("NEWYEAR10").build();
        when(discountService.getDiscounts()).thenReturn(List.of(res));

        mockMvc.perform(get("/discounts")
                        .with(user(cashierPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("NEWYEAR10"));
    }

    @Test
    void testUpdateDiscount_AsManager_Success() throws Exception {
        UpdateDiscountRequest request = UpdateDiscountRequest.builder()
                .name("NEWYEAR20")
                .discountType(DiscountType.PERCENTAGE)
                .value(new BigDecimal("20.00"))
                .active(true)
                .build();

        DiscountResponse response = DiscountResponse.builder().id(1L).name("NEWYEAR20").build();
        when(discountService.updateDiscount(eq(1L), any(UpdateDiscountRequest.class))).thenReturn(response);

        mockMvc.perform(put("/discounts/1")
                        .with(csrf())
                        .with(user(managerPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("NEWYEAR20"));
    }

    // ----------------------------------------------------
    // NOTIFICATION ENDPOINT TESTS
    // ----------------------------------------------------

    @Test
    void testGetNotifications_AsCashier_Success() throws Exception {
        NotificationResponse response = NotificationResponse.builder()
                .id(10L)
                .type(NotificationType.ORDER)
                .title("New Order Placed")
                .message("Message content")
                .readStatus(false)
                .build();

        when(notificationService.getNotifications()).thenReturn(List.of(response));

        mockMvc.perform(get("/notifications")
                        .with(user(cashierPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").value("New Order Placed"));
    }

    @Test
    void testGetUnreadCount_AsCashier_Success() throws Exception {
        when(notificationService.getUnreadCount()).thenReturn(new UnreadCountResponse(5L));

        mockMvc.perform(get("/notifications/unread-count")
                        .with(user(cashierPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.unreadCount").value(5));
    }

    @Test
    void testMarkAsRead_AsCashier_Success() throws Exception {
        NotificationResponse response = NotificationResponse.builder().id(10L).readStatus(true).build();
        when(notificationService.markAsRead(10L)).thenReturn(response);

        mockMvc.perform(patch("/notifications/10/read")
                        .with(csrf())
                        .with(user(cashierPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.readStatus").value(true));
    }

    @Test
    void testDeleteNotification_AsGuest_Forbidden() throws Exception {
        mockMvc.perform(delete("/notifications/10")
                        .with(csrf())
                        .with(user(guestPrincipal)))
                .andExpect(status().isForbidden());
    }

    // ----------------------------------------------------
    // SETTINGS ENDPOINT TESTS
    // ----------------------------------------------------

    @Test
    void testGetSettings_AsCashier_Success() throws Exception {
        MerchantSettingResponse response = MerchantSettingResponse.builder()
                .businessName("Cafe A")
                .contactEmail("cafea@test.com")
                .contactPhone("1234567890")
                .build();

        when(merchantSettingService.getSettings()).thenReturn(response);

        mockMvc.perform(get("/settings")
                        .with(user(cashierPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.businessName").value("Cafe A"));
    }

    @Test
    void testUpdateSettings_AsManager_Success() throws Exception {
        UpdateMerchantSettingRequest request = UpdateMerchantSettingRequest.builder()
                .businessName("Cafe A Updated")
                .contactEmail("cafea@test.com")
                .contactPhone("+123-456-7890")
                .address("123 Street")
                .build();

        MerchantSettingResponse response = MerchantSettingResponse.builder()
                .businessName("Cafe A Updated")
                .contactEmail("cafea@test.com")
                .contactPhone("+123-456-7890")
                .address("123 Street")
                .build();

        when(merchantSettingService.updateSettings(any(UpdateMerchantSettingRequest.class))).thenReturn(response);

        mockMvc.perform(put("/settings")
                        .with(csrf())
                        .with(user(managerPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.businessName").value("Cafe A Updated"));
    }

    @Test
    void testUpdateSettings_AsCashier_Forbidden() throws Exception {
        UpdateMerchantSettingRequest request = UpdateMerchantSettingRequest.builder()
                .businessName("Cafe A Updated")
                .contactEmail("cafea@test.com")
                .contactPhone("1234567890")
                .build();

        mockMvc.perform(put("/settings")
                        .with(csrf())
                        .with(user(cashierPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}

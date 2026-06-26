package com.campuscafe.backend.settings.service;

import com.campuscafe.backend.domain.merchant.Merchant;
import com.campuscafe.backend.domain.merchant.MerchantSetting;
import com.campuscafe.backend.domain.user.Role;
import com.campuscafe.backend.domain.user.User;
import com.campuscafe.backend.exception.AccessDeniedException;
import com.campuscafe.backend.repository.MerchantRepository;
import com.campuscafe.backend.repository.MerchantSettingRepository;
import com.campuscafe.backend.security.service.CustomUserDetails;
import com.campuscafe.backend.settings.dto.MerchantSettingResponse;
import com.campuscafe.backend.settings.dto.UpdateMerchantSettingRequest;
import com.campuscafe.backend.settings.mapper.MerchantSettingMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MerchantSettingServiceTest {

    @Mock
    private MerchantSettingRepository merchantSettingRepository;

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private MerchantSettingMapper merchantSettingMapper;

    @InjectMocks
    private MerchantSettingService merchantSettingService;

    private Merchant merchant;
    private User adminUser;
    private MerchantSetting setting;

    @BeforeEach
    void setUp() {
        merchant = Merchant.builder()
                .cafeName("Cafe A")
                .email("cafeA@test.com")
                .phone("1234567890")
                .address("123 Street")
                .verified(true)
                .build();
        merchant.setId(1L);

        Role adminRole = Role.builder().name("ADMIN").build();
        adminUser = User.builder().email("admin@cafeA.com").role(adminRole).merchant(merchant).active(true).build();
        adminUser.setId(1L);

        setting = MerchantSetting.builder()
                .merchant(merchant)
                .businessName("Cafe A")
                .contactEmail("cafeA@test.com")
                .contactPhone("1234567890")
                .address("123 Street")
                .build();
        setting.setId(100L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setupSecurityContext(User user) {
        CustomUserDetails userDetails = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void testGetSettings_Existing_Success() {
        setupSecurityContext(adminUser);

        when(merchantSettingRepository.findByMerchantId(1L)).thenReturn(Optional.of(setting));
        when(merchantSettingMapper.toResponse(setting)).thenReturn(
                MerchantSettingResponse.builder()
                        .businessName("Cafe A")
                        .contactEmail("cafeA@test.com")
                        .contactPhone("1234567890")
                        .address("123 Street")
                        .build()
        );

        MerchantSettingResponse result = merchantSettingService.getSettings();

        assertNotNull(result);
        assertEquals("Cafe A", result.getBusinessName());
        verify(merchantSettingRepository, never()).save(any(MerchantSetting.class));
    }

    @Test
    void testGetSettings_Missing_AutoProvisions_Success() {
        setupSecurityContext(adminUser);

        when(merchantSettingRepository.findByMerchantId(1L)).thenReturn(Optional.empty());
        when(merchantRepository.findById(1L)).thenReturn(Optional.of(merchant));
        when(merchantSettingRepository.save(any(MerchantSetting.class))).thenReturn(setting);
        when(merchantSettingMapper.toResponse(setting)).thenReturn(
                MerchantSettingResponse.builder()
                        .businessName("Cafe A")
                        .contactEmail("cafeA@test.com")
                        .contactPhone("1234567890")
                        .address("123 Street")
                        .build()
        );

        MerchantSettingResponse result = merchantSettingService.getSettings();

        assertNotNull(result);
        assertEquals("Cafe A", result.getBusinessName());
        verify(merchantSettingRepository, times(1)).save(any(MerchantSetting.class));
    }

    @Test
    void testUpdateSettings_Success() {
        setupSecurityContext(adminUser);

        UpdateMerchantSettingRequest request = UpdateMerchantSettingRequest.builder()
                .businessName("Cafe A Updated")
                .logoUrl("http://logo.com/image.png")
                .contactEmail("updated@cafea.com")
                .contactPhone("9876543210")
                .address("456 Avenue")
                .build();

        when(merchantSettingRepository.findByMerchantId(1L)).thenReturn(Optional.of(setting));
        when(merchantSettingRepository.save(any(MerchantSetting.class))).thenReturn(setting);

        MerchantSettingResponse response = MerchantSettingResponse.builder()
                .businessName("Cafe A Updated")
                .logoUrl("http://logo.com/image.png")
                .contactEmail("updated@cafea.com")
                .contactPhone("9876543210")
                .address("456 Avenue")
                .build();
        when(merchantSettingMapper.toResponse(setting)).thenReturn(response);

        MerchantSettingResponse result = merchantSettingService.updateSettings(request);

        assertNotNull(result);
        assertEquals("Cafe A Updated", result.getBusinessName());
        assertEquals("http://logo.com/image.png", result.getLogoUrl());
        assertEquals("updated@cafea.com", result.getContactEmail());
    }
}

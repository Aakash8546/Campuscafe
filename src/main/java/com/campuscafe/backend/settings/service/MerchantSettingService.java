package com.campuscafe.backend.settings.service;

import com.campuscafe.backend.domain.merchant.Merchant;
import com.campuscafe.backend.domain.merchant.MerchantSetting;
import com.campuscafe.backend.exception.AccessDeniedException;
import com.campuscafe.backend.exception.MerchantSettingsNotFoundException;
import com.campuscafe.backend.repository.MerchantRepository;
import com.campuscafe.backend.repository.MerchantSettingRepository;
import com.campuscafe.backend.security.service.CustomUserDetails;
import com.campuscafe.backend.settings.dto.MerchantSettingResponse;
import com.campuscafe.backend.settings.dto.UpdateMerchantSettingRequest;
import com.campuscafe.backend.settings.mapper.MerchantSettingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MerchantSettingService {

    private final MerchantSettingRepository merchantSettingRepository;
    private final MerchantRepository merchantRepository;
    private final MerchantSettingMapper merchantSettingMapper;

    private CustomUserDetails getAuthenticatedUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            throw new AccessDeniedException("User is not authenticated");
        }
        return (CustomUserDetails) authentication.getPrincipal();
    }

    private MerchantSetting getOrProvisionSettings(Long merchantId) {
        return merchantSettingRepository.findByMerchantId(merchantId)
                .orElseGet(() -> {
                    Merchant merchant = merchantRepository.findById(merchantId)
                            .orElseThrow(() -> new AccessDeniedException("Merchant not found"));

                    MerchantSetting defaultSetting = MerchantSetting.builder()
                            .merchant(merchant)
                            .businessName(merchant.getCafeName())
                            .logoUrl(null)
                            .contactEmail(merchant.getEmail())
                            .contactPhone(merchant.getPhone())
                            .address(merchant.getAddress())
                            .build();

                    return merchantSettingRepository.save(defaultSetting);
                });
    }

    public MerchantSettingResponse getSettings() {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        MerchantSetting setting = getOrProvisionSettings(merchantId);
        return merchantSettingMapper.toResponse(setting);
    }

    public MerchantSettingResponse updateSettings(UpdateMerchantSettingRequest request) {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        MerchantSetting setting = getOrProvisionSettings(merchantId);

        setting.setBusinessName(request.getBusinessName());
        setting.setLogoUrl(request.getLogoUrl());
        setting.setContactEmail(request.getContactEmail());
        setting.setContactPhone(request.getContactPhone());
        setting.setAddress(request.getAddress());

        MerchantSetting updated = merchantSettingRepository.save(setting);
        return merchantSettingMapper.toResponse(updated);
    }
}

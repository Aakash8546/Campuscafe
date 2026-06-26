package com.campuscafe.backend.settings.mapper;

import com.campuscafe.backend.domain.merchant.MerchantSetting;
import com.campuscafe.backend.settings.dto.MerchantSettingResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MerchantSettingMapper {

    MerchantSettingResponse toResponse(MerchantSetting setting);
}

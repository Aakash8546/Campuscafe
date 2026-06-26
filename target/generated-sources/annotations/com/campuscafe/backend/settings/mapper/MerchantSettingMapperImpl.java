package com.campuscafe.backend.settings.mapper;

import com.campuscafe.backend.domain.merchant.MerchantSetting;
import com.campuscafe.backend.settings.dto.MerchantSettingResponse;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-26T17:13:05+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.12 (Oracle Corporation)"
)
@Component
public class MerchantSettingMapperImpl implements MerchantSettingMapper {

    @Override
    public MerchantSettingResponse toResponse(MerchantSetting setting) {
        if ( setting == null ) {
            return null;
        }

        MerchantSettingResponse.MerchantSettingResponseBuilder merchantSettingResponse = MerchantSettingResponse.builder();

        merchantSettingResponse.businessName( setting.getBusinessName() );
        merchantSettingResponse.logoUrl( setting.getLogoUrl() );
        merchantSettingResponse.contactEmail( setting.getContactEmail() );
        merchantSettingResponse.contactPhone( setting.getContactPhone() );
        merchantSettingResponse.address( setting.getAddress() );

        return merchantSettingResponse.build();
    }
}

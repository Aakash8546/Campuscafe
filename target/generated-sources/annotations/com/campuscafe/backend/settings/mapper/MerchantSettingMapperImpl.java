package com.campuscafe.backend.settings.mapper;

import com.campuscafe.backend.domain.merchant.Merchant;
import com.campuscafe.backend.domain.merchant.MerchantSetting;
import com.campuscafe.backend.domain.merchant.enums.ShopStatus;
import com.campuscafe.backend.settings.dto.MerchantSettingResponse;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-26T22:35:37+0530",
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

        ShopStatus shopStatus = settingMerchantShopStatus( setting );
        if ( shopStatus != null ) {
            merchantSettingResponse.shopStatus( shopStatus.name() );
        }
        merchantSettingResponse.businessName( setting.getBusinessName() );
        merchantSettingResponse.logoUrl( setting.getLogoUrl() );
        merchantSettingResponse.contactEmail( setting.getContactEmail() );
        merchantSettingResponse.contactPhone( setting.getContactPhone() );
        merchantSettingResponse.address( setting.getAddress() );
        if ( setting.getPrinterSize() != null ) {
            merchantSettingResponse.printerSize( setting.getPrinterSize().name() );
        }
        merchantSettingResponse.upiQrUrl( setting.getUpiQrUrl() );

        return merchantSettingResponse.build();
    }

    private ShopStatus settingMerchantShopStatus(MerchantSetting merchantSetting) {
        if ( merchantSetting == null ) {
            return null;
        }
        Merchant merchant = merchantSetting.getMerchant();
        if ( merchant == null ) {
            return null;
        }
        ShopStatus shopStatus = merchant.getShopStatus();
        if ( shopStatus == null ) {
            return null;
        }
        return shopStatus;
    }
}

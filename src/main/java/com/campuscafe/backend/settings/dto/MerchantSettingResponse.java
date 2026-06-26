package com.campuscafe.backend.settings.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantSettingResponse {
    private String businessName;
    private String logoUrl;
    private String contactEmail;
    private String contactPhone;
    private String address;
    private String printerSize;
    private String upiQrUrl;
    private String shopStatus;
}

package com.campuscafe.backend.domain.merchant;

import com.campuscafe.backend.domain.base.BaseEntity;
import com.campuscafe.backend.domain.merchant.enums.PrinterSize;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "merchant_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantSetting extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "printer_size", nullable = false, length = 10)
    @Builder.Default
    private PrinterSize printerSize = PrinterSize.SIZE_80MM;

    @Column(name = "upi_qr_url")
    private String upiQrUrl;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false, unique = true)
    private Merchant merchant;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "business_name", nullable = false, length = 100)
    private String businessName;

    @Column(name = "contact_email", nullable = false, length = 100)
    private String contactEmail;

    @Column(name = "contact_phone", nullable = false, length = 20)
    private String contactPhone;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;
}

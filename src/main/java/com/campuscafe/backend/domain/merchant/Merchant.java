package com.campuscafe.backend.domain.merchant;

import com.campuscafe.backend.domain.base.BaseEntity;
import com.campuscafe.backend.domain.merchant.enums.ShopStatus;
import com.campuscafe.backend.domain.merchant.enums.VerificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "merchants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Merchant extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "shop_status", nullable = false, length = 10)
    @Builder.Default
    private ShopStatus shopStatus = ShopStatus.OPEN;

    @Column(name = "cafe_name", nullable = false, length = 100)
    private String cafeName;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "city", length = 50)
    private String city;

    @Column(name = "pincode", length = 10)
    private String pincode;

    @Enumerated(EnumType.STRING)
    @Column(name = "verified", nullable = false, length = 20)
    @Builder.Default
    private VerificationStatus verified = VerificationStatus.PENDING;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    @Column(name = "super_admin_token", length = 100)
    private String superAdminToken;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "next_bill_serial", nullable = false)
    @Builder.Default
    private Long nextBillSerial = 0L;

    public static class MerchantBuilder {
        private VerificationStatus verified;
        private Boolean emailVerified;

        public MerchantBuilder verified(Boolean verified) {
            this.verified = verified ? VerificationStatus.VERIFIED : VerificationStatus.PENDING;
            this.emailVerified = verified; // maintain emailVerified status matching older boolean tests
            return this;
        }

        public MerchantBuilder verified(VerificationStatus verified) {
            this.verified = verified;
            return this;
        }
    }
}

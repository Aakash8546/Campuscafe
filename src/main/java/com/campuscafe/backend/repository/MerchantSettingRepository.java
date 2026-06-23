package com.campuscafe.backend.repository;

import com.campuscafe.backend.domain.merchant.MerchantSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MerchantSettingRepository extends JpaRepository<MerchantSetting, Long> {
    Optional<MerchantSetting> findByMerchantId(Long merchantId);
}

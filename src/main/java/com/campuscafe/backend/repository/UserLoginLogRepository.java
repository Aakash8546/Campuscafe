package com.campuscafe.backend.repository;

import com.campuscafe.backend.domain.user.UserLoginLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface UserLoginLogRepository extends JpaRepository<UserLoginLog, Long>, JpaSpecificationExecutor<UserLoginLog> {
    Page<UserLoginLog> findByMerchantIdOrderByLoginTimeDesc(Long merchantId, Pageable pageable);
}

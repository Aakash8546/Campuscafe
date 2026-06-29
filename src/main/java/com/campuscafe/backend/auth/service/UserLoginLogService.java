package com.campuscafe.backend.auth.service;

import com.campuscafe.backend.auth.dto.UserLoginLogResponse;
import com.campuscafe.backend.domain.merchant.Merchant;
import com.campuscafe.backend.domain.user.LoginStatus;
import com.campuscafe.backend.domain.user.User;
import com.campuscafe.backend.domain.user.UserLoginLog;
import com.campuscafe.backend.repository.UserLoginLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserLoginLogService {

    private final UserLoginLogRepository userLoginLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLog(Merchant merchant, User user, String email, String ipAddress, String userAgent, LoginStatus status, String failureReason) {
        try {
            UserLoginLog loginLog = UserLoginLog.builder()
                    .merchant(merchant)
                    .user(user)
                    .userName(user != null ? user.getName() : null)
                    .userRole(user != null && user.getRole() != null ? user.getRole().getName() : null)
                    .email(email)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .status(status)
                    .failureReason(failureReason)
                    .loginTime(Instant.now())
                    .build();
            userLoginLogRepository.save(loginLog);
        } catch (Exception e) {
            log.error("Failed to record user login log for email: {}", email, e);
        }
    }

    @Transactional(readOnly = true)
    public Page<UserLoginLogResponse> getLoginLogs(Long merchantId, Pageable pageable) {
        return userLoginLogRepository.findByMerchantIdOrderByLoginTimeDesc(merchantId, pageable)
                .map(log -> UserLoginLogResponse.builder()
                        .id(log.getId())
                        .merchantId(log.getMerchant() != null ? log.getMerchant().getId() : null)
                        .userId(log.getUser() != null ? log.getUser().getId() : null)
                        .userName(log.getUserName())
                        .userRole(log.getUserRole())
                        .email(log.getEmail())
                        .ipAddress(log.getIpAddress())
                        .userAgent(log.getUserAgent())
                        .status(log.getStatus())
                        .failureReason(log.getFailureReason())
                        .loginTime(log.getLoginTime())
                        .build());
    }
}

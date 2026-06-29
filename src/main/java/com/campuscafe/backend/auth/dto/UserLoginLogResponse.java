package com.campuscafe.backend.auth.dto;

import com.campuscafe.backend.domain.user.LoginStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginLogResponse {
    private Long id;
    private Long merchantId;
    private Long userId;
    private String userName;
    private String userRole;
    private String email;
    private String ipAddress;
    private String userAgent;
    private LoginStatus status;
    private String failureReason;
    private Instant loginTime;
}

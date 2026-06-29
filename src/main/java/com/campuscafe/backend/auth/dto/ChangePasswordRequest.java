package com.campuscafe.backend.auth.dto;

import com.campuscafe.backend.auth.validator.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangePasswordRequest {

    @NotBlank(message = "Current password is required")
    @Size(max = 128, message = "Password must be less than 128 characters")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(max = 128, message = "Password must be less than 128 characters")
    @ValidPassword
    private String newPassword;
}

package com.campuscafe.backend.auth.dto;

import com.campuscafe.backend.auth.validator.ValidPassword;
import jakarta.validation.constraints.Email;
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
public class SignupRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must be less than 100 characters")
    private String fullName;

    @NotBlank(message = "Cafe name is required")
    @Size(max = 100, message = "Cafe name must be less than 100 characters")
    private String cafeName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must be less than 100 characters")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Size(max = 20, message = "Phone number must be less than 20 characters")
    private String phone;

    @NotBlank(message = "Password is required")
    @Size(max = 128, message = "Password must be less than 128 characters")
    @ValidPassword
    private String password;

    @NotBlank(message = "Confirm password is required")
    @Size(max = 128, message = "Confirm password must be less than 128 characters")
    private String confirmPassword;

    @NotBlank(message = "Cafe address is required")
    private String address;

    @NotBlank(message = "City is required")
    @Size(max = 50, message = "City must be less than 50 characters")
    private String city;

    @NotBlank(message = "Pincode is required")
    @Size(max = 10, message = "Pincode must be less than 10 characters")
    private String pincode;
}

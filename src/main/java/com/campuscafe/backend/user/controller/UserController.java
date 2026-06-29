package com.campuscafe.backend.user.controller;

import com.campuscafe.backend.common.ApiResponse;
import com.campuscafe.backend.user.dto.CreateUserRequest;
import com.campuscafe.backend.user.dto.UpdateUserRequest;
import com.campuscafe.backend.user.dto.UserResponse;
import com.campuscafe.backend.user.dto.UserSummaryResponse;
import com.campuscafe.backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.campuscafe.backend.auth.dto.UserLoginLogResponse;
import com.campuscafe.backend.auth.service.UserLoginLogService;
import com.campuscafe.backend.security.service.CustomUserDetails;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Validated
@Tag(name = "User Management", description = "Endpoints for multi-tenant user CRUD operations and activation/deactivation")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;
    private final UserLoginLogService userLoginLogService;

    @GetMapping("/login-logs")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    @Operation(summary = "Get employee login history and audit logs", description = "Accessible by ADMIN and MANAGER.")
    public ResponseEntity<ApiResponse<Page<UserLoginLogResponse>>> getLoginLogs(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @ParameterObject Pageable pageable
    ) {
        Page<UserLoginLogResponse> response = userLoginLogService.getLoginLogs(userDetails.getMerchantId(), pageable);
        return ResponseEntity.ok(ApiResponse.success("Login audit logs retrieved successfully", response));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE')")
    @Operation(summary = "Create a new user under the authenticated merchant", description = "Only accessible by ADMIN.")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.ok(ApiResponse.success("User created successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER_VIEW')")
    @Operation(summary = "Get all users belonging to the authenticated merchant", description = "Accessible by ADMIN and MANAGER.")
    public ResponseEntity<ApiResponse<List<UserSummaryResponse>>> getAllUsers() {
        List<UserSummaryResponse> response = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    @Operation(summary = "Get detailed profile of a user by ID", description = "User must belong to the authenticated merchant. Accessible by ADMIN and MANAGER.")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    @Operation(summary = "Update user details", description = "User must belong to the authenticated merchant. Only accessible by ADMIN.")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        UserResponse response = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", response));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    @Operation(summary = "Deactivate a user", description = "User must belong to the authenticated merchant. Only accessible by ADMIN.")
    public ResponseEntity<ApiResponse<UserResponse>> deactivateUser(@PathVariable Long id) {
        UserResponse response = userService.deactivateUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deactivated successfully", response));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    @Operation(summary = "Activate a user", description = "User must belong to the authenticated merchant. Only accessible by ADMIN.")
    public ResponseEntity<ApiResponse<UserResponse>> activateUser(@PathVariable Long id) {
        UserResponse response = userService.activateUser(id);
        return ResponseEntity.ok(ApiResponse.success("User activated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    @Operation(summary = "Delete a user", description = "User must belong to the authenticated merchant. Only accessible by ADMIN.")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
    }
}

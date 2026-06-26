package com.campuscafe.backend.settings.controller;

import com.campuscafe.backend.common.ApiResponse;
import com.campuscafe.backend.settings.dto.MerchantSettingResponse;
import com.campuscafe.backend.settings.dto.UpdateMerchantSettingRequest;
import com.campuscafe.backend.settings.service.MerchantSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
@Validated
@Tag(name = "Merchant Settings Management", description = "Endpoints for retrieving and updating merchant-level branding and contact settings")
@SecurityRequirement(name = "bearerAuth")
public class MerchantSettingController {

    private final MerchantSettingService merchantSettingService;

    @GetMapping
    @PreAuthorize("hasAuthority('SETTINGS_VIEW')")
    @Operation(summary = "Get merchant settings", description = "Requires SETTINGS_VIEW. Provisions default settings if missing.")
    public ResponseEntity<ApiResponse<MerchantSettingResponse>> getSettings() {
        MerchantSettingResponse response = merchantSettingService.getSettings();
        return ResponseEntity.ok(ApiResponse.success("Merchant settings retrieved successfully", response));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('SETTINGS_UPDATE')")
    @Operation(summary = "Update merchant settings", description = "Requires SETTINGS_UPDATE. Provisions default settings if missing.")
    public ResponseEntity<ApiResponse<MerchantSettingResponse>> updateSettings(
            @Valid @RequestBody UpdateMerchantSettingRequest request
    ) {
        MerchantSettingResponse response = merchantSettingService.updateSettings(request);
        return ResponseEntity.ok(ApiResponse.success("Merchant settings updated successfully", response));
    }
}

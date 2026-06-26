package com.campuscafe.backend.notification.controller;

import com.campuscafe.backend.common.ApiResponse;
import com.campuscafe.backend.notification.dto.NotificationResponse;
import com.campuscafe.backend.notification.dto.UnreadCountResponse;
import com.campuscafe.backend.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Validated
@Tag(name = "Notification Management", description = "Endpoints for retrieving and managing system and order notifications")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW')")
    @Operation(summary = "Get all notifications for the merchant", description = "Requires NOTIFICATION_VIEW.")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotifications() {
        List<NotificationResponse> response = notificationService.getNotifications();
        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved successfully", response));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW')")
    @Operation(summary = "Get count of unread notifications", description = "Requires NOTIFICATION_VIEW.")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount() {
        UnreadCountResponse response = notificationService.getUnreadCount();
        return ResponseEntity.ok(ApiResponse.success("Unread count retrieved successfully", response));
    }

    @GetMapping("/recent")
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW')")
    @Operation(summary = "Get top 10 recent notifications", description = "Requires NOTIFICATION_VIEW.")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getRecentNotifications() {
        List<NotificationResponse> response = notificationService.getRecentNotifications();
        return ResponseEntity.ok(ApiResponse.success("Recent notifications retrieved successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW')")
    @Operation(summary = "Get notification details by ID", description = "Requires NOTIFICATION_VIEW.")
    public ResponseEntity<ApiResponse<NotificationResponse>> getNotificationById(
            @PathVariable Long id
    ) {
        NotificationResponse response = notificationService.getNotificationById(id);
        return ResponseEntity.ok(ApiResponse.success("Notification retrieved successfully", response));
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW')")
    @Operation(summary = "Mark single notification as read", description = "Requires NOTIFICATION_VIEW.")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @PathVariable Long id
    ) {
        NotificationResponse response = notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read successfully", response));
    }

    @PatchMapping("/read-all")
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW')")
    @Operation(summary = "Mark all notifications as read", description = "Requires NOTIFICATION_VIEW.")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read successfully", null));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW')")
    @Operation(summary = "Delete notification by ID", description = "Requires NOTIFICATION_VIEW.")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @PathVariable Long id
    ) {
        notificationService.deleteNotification(id);
        return ResponseEntity.ok(ApiResponse.success("Notification deleted successfully", null));
    }
}

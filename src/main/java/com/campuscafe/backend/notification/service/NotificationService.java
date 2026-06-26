package com.campuscafe.backend.notification.service;

import com.campuscafe.backend.domain.notification.Notification;
import com.campuscafe.backend.exception.AccessDeniedException;
import com.campuscafe.backend.exception.NotificationNotFoundException;
import com.campuscafe.backend.notification.dto.NotificationResponse;
import com.campuscafe.backend.notification.dto.UnreadCountResponse;
import com.campuscafe.backend.notification.mapper.NotificationMapper;
import com.campuscafe.backend.repository.NotificationRepository;
import com.campuscafe.backend.security.service.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    private CustomUserDetails getAuthenticatedUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            throw new AccessDeniedException("User is not authenticated");
        }
        return (CustomUserDetails) authentication.getPrincipal();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications() {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        List<Notification> notifications = notificationRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId);
        return notificationMapper.toResponseList(notifications);
    }

    @Transactional(readOnly = true)
    public NotificationResponse getNotificationById(Long id) {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        Notification notification = notificationRepository.findByIdAndMerchantId(id, merchantId)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found with id: " + id));

        return notificationMapper.toResponse(notification);
    }

    public NotificationResponse markAsRead(Long id) {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        Notification notification = notificationRepository.findByIdAndMerchantId(id, merchantId)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found with id: " + id));

        notification.setReadStatus(true);
        Notification updated = notificationRepository.save(notification);
        return notificationMapper.toResponse(updated);
    }

    public void markAllAsRead() {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        List<Notification> unreadNotifications = notificationRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId);
        for (Notification notification : unreadNotifications) {
            if (!notification.getReadStatus()) {
                notification.setReadStatus(true);
            }
        }
        notificationRepository.saveAll(unreadNotifications);
    }

    public void deleteNotification(Long id) {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        Notification notification = notificationRepository.findByIdAndMerchantId(id, merchantId)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found with id: " + id));

        notificationRepository.delete(notification);
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount() {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        long count = notificationRepository.countByMerchantIdAndReadStatus(merchantId, false);
        return new UnreadCountResponse(count);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getRecentNotifications() {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        List<Notification> notifications = notificationRepository.findTop10ByMerchantIdOrderByCreatedAtDesc(merchantId);
        return notificationMapper.toResponseList(notifications);
    }
}

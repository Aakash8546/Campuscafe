package com.campuscafe.backend.notification.service;

import com.campuscafe.backend.domain.merchant.Merchant;
import com.campuscafe.backend.domain.notification.Notification;
import com.campuscafe.backend.domain.notification.enums.NotificationType;
import com.campuscafe.backend.domain.user.Role;
import com.campuscafe.backend.domain.user.User;
import com.campuscafe.backend.exception.AccessDeniedException;
import com.campuscafe.backend.exception.NotificationNotFoundException;
import com.campuscafe.backend.notification.dto.NotificationResponse;
import com.campuscafe.backend.notification.dto.UnreadCountResponse;
import com.campuscafe.backend.notification.mapper.NotificationMapper;
import com.campuscafe.backend.repository.NotificationRepository;
import com.campuscafe.backend.security.service.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationService notificationService;

    private Merchant merchant;
    private User adminUser;
    private Notification n1;
    private Notification n2;

    @BeforeEach
    void setUp() {
        merchant = Merchant.builder().cafeName("Cafe A").email("cafeA@test.com").verified(true).build();
        merchant.setId(1L);

        Role adminRole = Role.builder().name("ADMIN").build();
        adminUser = User.builder().email("admin@cafeA.com").role(adminRole).merchant(merchant).active(true).build();
        adminUser.setId(1L);

        n1 = Notification.builder()
                .merchant(merchant)
                .type(NotificationType.ORDER)
                .title("Order Placed")
                .message("New order ORD-1")
                .readStatus(false)
                .createdAt(Instant.now())
                .build();
        n1.setId(10L);

        n2 = Notification.builder()
                .merchant(merchant)
                .type(NotificationType.LOW_STOCK)
                .title("Low Stock Warning")
                .message("Item X low stock")
                .readStatus(false)
                .createdAt(Instant.now())
                .build();
        n2.setId(20L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setupSecurityContext(User user) {
        CustomUserDetails userDetails = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void testGetNotifications_Success() {
        setupSecurityContext(adminUser);

        when(notificationRepository.findByMerchantIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(n1, n2));
        when(notificationMapper.toResponseList(anyList())).thenReturn(List.of(
                NotificationResponse.builder().id(10L).title("Order Placed").build(),
                NotificationResponse.builder().id(20L).title("Low Stock Warning").build()
        ));

        List<NotificationResponse> result = notificationService.getNotifications();

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testGetNotificationById_Success() {
        setupSecurityContext(adminUser);

        when(notificationRepository.findByIdAndMerchantId(10L, 1L)).thenReturn(Optional.of(n1));
        when(notificationMapper.toResponse(n1)).thenReturn(
                NotificationResponse.builder().id(10L).title("Order Placed").build()
        );

        NotificationResponse result = notificationService.getNotificationById(10L);

        assertNotNull(result);
        assertEquals("Order Placed", result.getTitle());
    }

    @Test
    void testGetNotificationById_NotFound_ThrowsNotificationNotFoundException() {
        setupSecurityContext(adminUser);

        when(notificationRepository.findByIdAndMerchantId(999L, 1L)).thenReturn(Optional.empty());

        assertThrows(NotificationNotFoundException.class, () -> notificationService.getNotificationById(999L));
    }

    @Test
    void testMarkAsRead_Success() {
        setupSecurityContext(adminUser);

        when(notificationRepository.findByIdAndMerchantId(10L, 1L)).thenReturn(Optional.of(n1));
        when(notificationRepository.save(any(Notification.class))).thenReturn(n1);

        NotificationResponse response = NotificationResponse.builder().id(10L).readStatus(true).build();
        when(notificationMapper.toResponse(any(Notification.class))).thenReturn(response);

        NotificationResponse result = notificationService.markAsRead(10L);

        assertNotNull(result);
        assertTrue(result.getReadStatus());
        assertTrue(n1.getReadStatus());
    }

    @Test
    void testMarkAllAsRead_Success() {
        setupSecurityContext(adminUser);

        when(notificationRepository.findByMerchantIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(n1, n2));

        notificationService.markAllAsRead();

        assertTrue(n1.getReadStatus());
        assertTrue(n2.getReadStatus());
        verify(notificationRepository, times(1)).saveAll(anyList());
    }

    @Test
    void testDeleteNotification_Success() {
        setupSecurityContext(adminUser);

        when(notificationRepository.findByIdAndMerchantId(10L, 1L)).thenReturn(Optional.of(n1));

        notificationService.deleteNotification(10L);

        verify(notificationRepository, times(1)).delete(n1);
    }

    @Test
    void testGetUnreadCount_Success() {
        setupSecurityContext(adminUser);

        when(notificationRepository.countByMerchantIdAndReadStatus(1L, false)).thenReturn(5L);

        UnreadCountResponse result = notificationService.getUnreadCount();

        assertNotNull(result);
        assertEquals(5L, result.getUnreadCount());
    }

    @Test
    void testGetRecentNotifications_Success() {
        setupSecurityContext(adminUser);

        when(notificationRepository.findTop10ByMerchantIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(n1));
        when(notificationMapper.toResponseList(anyList())).thenReturn(List.of(
                NotificationResponse.builder().id(10L).title("Order Placed").build()
        ));

        List<NotificationResponse> result = notificationService.getRecentNotifications();

        assertNotNull(result);
        assertEquals(1, result.size());
    }
}

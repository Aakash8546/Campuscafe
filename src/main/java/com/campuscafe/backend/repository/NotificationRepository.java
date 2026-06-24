package com.campuscafe.backend.repository;

import com.campuscafe.backend.domain.notification.Notification;
import com.campuscafe.backend.domain.notification.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsByMerchantIdAndTypeAndTitleAndReadStatus(
            Long merchantId,
            NotificationType type,
            String title,
            Boolean readStatus
    );
}

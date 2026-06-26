package com.campuscafe.backend.repository;

import com.campuscafe.backend.domain.notification.Notification;
import com.campuscafe.backend.domain.notification.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsByMerchantIdAndTypeAndTitleAndReadStatus(
            Long merchantId,
            NotificationType type,
            String title,
            Boolean readStatus
    );

    List<Notification> findByMerchantIdOrderByCreatedAtDesc(Long merchantId);

    Optional<Notification> findByIdAndMerchantId(Long id, Long merchantId);

    long countByMerchantIdAndReadStatus(Long merchantId, Boolean readStatus);

    List<Notification> findTop10ByMerchantIdOrderByCreatedAtDesc(Long merchantId);
}

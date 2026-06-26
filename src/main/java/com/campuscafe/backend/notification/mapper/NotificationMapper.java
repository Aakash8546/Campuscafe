package com.campuscafe.backend.notification.mapper;

import com.campuscafe.backend.domain.notification.Notification;
import com.campuscafe.backend.notification.dto.NotificationResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationResponse toResponse(Notification notification);

    List<NotificationResponse> toResponseList(List<Notification> notifications);
}

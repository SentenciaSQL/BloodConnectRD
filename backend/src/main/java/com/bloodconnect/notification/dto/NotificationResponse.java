package com.bloodconnect.notification.dto;

import com.bloodconnect.common.enums.NotificationType;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String message,
        String resourceType,
        Long resourceId,
        boolean read,
        Instant createdAt
) {
}

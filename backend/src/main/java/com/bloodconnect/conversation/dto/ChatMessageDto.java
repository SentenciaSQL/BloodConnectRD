package com.bloodconnect.conversation.dto;

import com.bloodconnect.common.enums.MessageStatus;

import java.time.Instant;

public record ChatMessageDto(
        Long id,
        Long conversationId,
        Long senderId,
        String senderName,
        String body,
        boolean mine,
        MessageStatus status,
        Instant deliveredAt,
        Instant readAt,
        Instant createdAt
) {
}

package com.bloodconnect.conversation.dto;

import java.time.Instant;

public record ChatMessageDto(
        Long id,
        Long conversationId,
        Long senderId,
        String senderName,
        String body,
        boolean mine,
        Instant createdAt
) {
}

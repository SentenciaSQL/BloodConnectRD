package com.bloodconnect.conversation.dto;

import com.bloodconnect.common.enums.BloodType;

import java.time.Instant;

public record ConversationDto(
        Long id,
        Long bloodRequestId,
        String bloodRequestPatientName,
        String bloodRequestHospital,
        BloodType bloodRequestBloodType,
        Long ownerUserId,
        Long donorUserId,
        Long otherUserId,
        String otherUserName,
        String lastMessage,
        Instant lastMessageAt,
        long unreadCount,
        Instant createdAt,
        Instant updatedAt
) {
}

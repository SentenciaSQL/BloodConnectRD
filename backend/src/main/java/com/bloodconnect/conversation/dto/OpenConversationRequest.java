package com.bloodconnect.conversation.dto;

import jakarta.validation.constraints.NotNull;

public record OpenConversationRequest(
        @NotNull(message = "Debe indicar el donante a contactar") Long donorUserId
) {
}

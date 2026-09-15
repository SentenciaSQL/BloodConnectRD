package com.bloodconnect.assistant.dto;

import java.util.List;

public record AssistantAskResponse(
        String reply,
        String toolUsed,
        List<AssistantLinkDto> links
) {
    public AssistantAskResponse {
        if (links == null) {
            links = List.of();
        }
    }
}

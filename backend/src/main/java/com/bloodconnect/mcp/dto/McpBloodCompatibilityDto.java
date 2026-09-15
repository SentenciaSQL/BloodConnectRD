package com.bloodconnect.mcp.dto;

import com.bloodconnect.common.enums.BloodType;

public record McpBloodCompatibilityDto(
        BloodType donorBloodType,
        BloodType recipientBloodType,
        boolean compatible,
        String explanation,
        String disclaimer
) {
}

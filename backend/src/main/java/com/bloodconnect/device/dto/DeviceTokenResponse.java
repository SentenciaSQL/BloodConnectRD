package com.bloodconnect.device.dto;

import com.bloodconnect.common.enums.DevicePlatform;

import java.time.Instant;

public record DeviceTokenResponse(
        Long id,
        String token,
        DevicePlatform platform,
        Instant createdAt,
        Instant updatedAt
) {
}

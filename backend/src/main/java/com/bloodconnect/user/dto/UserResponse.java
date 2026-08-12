package com.bloodconnect.user.dto;

import com.bloodconnect.common.enums.Role;

import java.time.Instant;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        Role role,
        boolean enabled,
        Instant createdAt
) {
}

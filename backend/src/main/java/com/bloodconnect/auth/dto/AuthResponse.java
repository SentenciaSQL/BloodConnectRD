package com.bloodconnect.auth.dto;

import com.bloodconnect.user.dto.UserResponse;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {

    public AuthResponse(
            String accessToken,
            String refreshToken,
            long expiresIn,
            UserResponse user
    ) {
        this(accessToken, refreshToken, "Bearer", expiresIn, user);
    }
}

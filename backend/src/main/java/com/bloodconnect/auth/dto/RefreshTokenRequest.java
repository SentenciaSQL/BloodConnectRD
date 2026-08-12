package com.bloodconnect.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "El token de actualización es obligatorio")
        String refreshToken
) {
}

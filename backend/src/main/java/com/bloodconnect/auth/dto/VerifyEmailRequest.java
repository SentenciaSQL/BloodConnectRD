package com.bloodconnect.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyEmailRequest(
        @NotBlank(message = "El token es obligatorio")
        String token
) {
}

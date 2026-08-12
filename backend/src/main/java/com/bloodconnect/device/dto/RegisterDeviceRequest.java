package com.bloodconnect.device.dto;

import com.bloodconnect.common.enums.DevicePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterDeviceRequest(
        @NotBlank(message = "El token del dispositivo es obligatorio")
        @Size(max = 512, message = "El token no puede exceder 512 caracteres") String token,
        @NotNull(message = "La plataforma es obligatoria") DevicePlatform platform
) {
}

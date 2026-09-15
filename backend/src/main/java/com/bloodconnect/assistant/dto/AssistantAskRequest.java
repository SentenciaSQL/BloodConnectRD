package com.bloodconnect.assistant.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssistantAskRequest(
        @NotBlank(message = "El mensaje no puede estar vacío")
        @Size(max = 500, message = "El mensaje no puede exceder 500 caracteres")
        String message,
        @DecimalMin(value = "-90.0", message = "La latitud no es válida")
        @DecimalMax(value = "90.0", message = "La latitud no es válida")
        Double latitude,
        @DecimalMin(value = "-180.0", message = "La longitud no es válida")
        @DecimalMax(value = "180.0", message = "La longitud no es válida")
        Double longitude
) {
}

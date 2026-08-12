package com.bloodconnect.admin.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(
        @NotNull(message = "El estado de la cuenta es obligatorio") Boolean enabled
) {
}

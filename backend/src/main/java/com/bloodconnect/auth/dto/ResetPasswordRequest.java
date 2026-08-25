package com.bloodconnect.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(

        @NotBlank(message = "El token es obligatorio")
        String token,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(
                min = 8,
                max = 100,
                message = "La contraseña debe tener entre 8 y 100 caracteres"
        )
        String password,

        @NotBlank(message = "Debe confirmar la contraseña")
        String confirmPassword

) {
}

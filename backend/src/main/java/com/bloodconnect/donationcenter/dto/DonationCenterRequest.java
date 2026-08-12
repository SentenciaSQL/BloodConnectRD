package com.bloodconnect.donationcenter.dto;

import com.bloodconnect.common.enums.CenterType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record DonationCenterRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 200, message = "El nombre no puede exceder 200 caracteres") String name,
        @NotNull(message = "El tipo de centro es obligatorio") CenterType type,
        @NotNull(message = "La provincia es obligatoria")
        @Positive(message = "La provincia no es válida") Long provinceId,
        @NotNull(message = "El municipio es obligatorio")
        @Positive(message = "El municipio no es válido") Long municipalityId,
        @Size(max = 120, message = "El sector no puede exceder 120 caracteres") String sector,
        @NotBlank(message = "La dirección es obligatoria")
        @Size(max = 255, message = "La dirección no puede exceder 255 caracteres") String address,
        @Size(max = 255, message = "La referencia no puede exceder 255 caracteres") String reference,
        @Size(max = 30, message = "El teléfono no puede exceder 30 caracteres") String phone,
        @Size(max = 255, message = "El horario no puede exceder 255 caracteres") String schedule,
        @DecimalMin(value = "-90", message = "La latitud no es válida")
        @DecimalMax(value = "90", message = "La latitud no es válida") BigDecimal latitude,
        @DecimalMin(value = "-180", message = "La longitud no es válida")
        @DecimalMax(value = "180", message = "La longitud no es válida") BigDecimal longitude,
        Boolean active
) {
}

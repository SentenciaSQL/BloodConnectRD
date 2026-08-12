package com.bloodconnect.bloodrequest.dto;

import com.bloodconnect.common.enums.BloodType;
import com.bloodconnect.common.enums.Urgency;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateBloodRequest(
        @NotBlank(message = "El nombre del paciente es obligatorio")
        @Size(max = 200, message = "El nombre del paciente no puede exceder 200 caracteres") String patientName,
        @NotNull(message = "El tipo sanguíneo es obligatorio") BloodType bloodType,
        @Positive(message = "Las unidades requeridas deben ser mayores que cero") int unitsRequired,
        @NotBlank(message = "El hospital es obligatorio")
        @Size(max = 200, message = "El hospital no puede exceder 200 caracteres") String hospital,
        @NotNull(message = "La provincia es obligatoria")
        @Positive(message = "La provincia no es válida") Long provinceId,
        @NotNull(message = "El municipio es obligatorio")
        @Positive(message = "El municipio no es válido") Long municipalityId,
        @Size(max = 120, message = "El sector no puede exceder 120 caracteres") String sector,
        @NotBlank(message = "La dirección es obligatoria")
        @Size(max = 255, message = "La dirección no puede exceder 255 caracteres") String address,
        @Size(max = 255, message = "La referencia no puede exceder 255 caracteres") String reference,
        @DecimalMin(value = "-90", message = "La latitud no es válida")
        @DecimalMax(value = "90", message = "La latitud no es válida") BigDecimal latitude,
        @DecimalMin(value = "-180", message = "La longitud no es válida")
        @DecimalMax(value = "180", message = "La longitud no es válida") BigDecimal longitude,
        @NotNull(message = "La fecha límite es obligatoria")
        @Future(message = "La fecha límite debe estar en el futuro") Instant deadline,
        @Size(max = 4000, message = "La descripción no puede exceder 4000 caracteres") String description,
        @NotBlank(message = "El teléfono de contacto es obligatorio")
        @Size(max = 30, message = "El teléfono no puede exceder 30 caracteres") String contactPhone,
        @NotNull(message = "La urgencia es obligatoria") Urgency urgency
) {
}

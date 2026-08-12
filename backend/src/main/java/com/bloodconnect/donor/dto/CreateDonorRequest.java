package com.bloodconnect.donor.dto;

import com.bloodconnect.common.enums.BloodType;
import com.bloodconnect.common.enums.Sex;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateDonorRequest(
        @NotNull(message = "El tipo sanguíneo es obligatorio") BloodType bloodType,
        @NotNull(message = "La fecha de nacimiento es obligatoria")
        @Past(message = "La fecha de nacimiento debe estar en el pasado") LocalDate birthDate,
        @NotNull(message = "El sexo es obligatorio") Sex sex,
        @NotBlank(message = "El teléfono es obligatorio")
        @Size(max = 30, message = "El teléfono no puede exceder 30 caracteres") String phone,
        @NotNull(message = "La provincia es obligatoria")
        @Positive(message = "La provincia no es válida") Long provinceId,
        @NotNull(message = "El municipio es obligatorio")
        @Positive(message = "El municipio no es válido") Long municipalityId,
        @Size(max = 120, message = "El sector no puede exceder 120 caracteres") String sector,
        @Size(max = 255, message = "La dirección no puede exceder 255 caracteres") String approximateAddress,
        @DecimalMin(value = "-90", message = "La latitud no es válida")
        @DecimalMax(value = "90", message = "La latitud no es válida") BigDecimal latitude,
        @DecimalMin(value = "-180", message = "La longitud no es válida")
        @DecimalMax(value = "180", message = "La longitud no es válida") BigDecimal longitude,
        LocalDate lastDonationDate
) {
}

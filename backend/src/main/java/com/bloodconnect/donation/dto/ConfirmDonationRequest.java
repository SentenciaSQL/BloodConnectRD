package com.bloodconnect.donation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ConfirmDonationRequest(
        @NotNull(message = "Debe indicar las unidades recibidas")
        @Positive(message = "Las unidades recibidas deben ser mayores que cero")
        Integer confirmedUnits
) {
}

package com.bloodconnect.donation.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ReportDonationRequest(
        @Positive(message = "Las unidades donadas deben ser mayores que cero") int units,
        LocalDate donationDate,
        @Size(max = 500, message = "Las notas no pueden exceder 500 caracteres") String notes
) {
}

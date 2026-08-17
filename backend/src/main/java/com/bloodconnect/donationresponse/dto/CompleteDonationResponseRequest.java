package com.bloodconnect.donationresponse.dto;

import jakarta.validation.constraints.Positive;

public record CompleteDonationResponseRequest(
        @Positive(message = "Las unidades donadas deben ser mayores que cero") Integer units
) {
}

package com.bloodconnect.donationresponse.dto;

import jakarta.validation.constraints.Size;

public record CreateDonationResponseRequest(
        @Size(max = 500, message = "El mensaje no puede exceder 500 caracteres") String message
) {
}

package com.bloodconnect.donation.dto;

import com.bloodconnect.common.enums.DonationStatus;

import java.time.Instant;
import java.time.LocalDate;

public record DonationDto(
        Long id,
        Long donorId,
        String donorName,
        Long bloodRequestId,
        Long donationCenterId,
        String donationCenterName,
        LocalDate donationDate,
        int units,
        String notes,
        DonationStatus status,
        Instant createdAt
) {
}

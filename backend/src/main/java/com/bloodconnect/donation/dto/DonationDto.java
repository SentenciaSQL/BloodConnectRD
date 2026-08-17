package com.bloodconnect.donation.dto;

import com.bloodconnect.common.enums.DonationStatus;

import java.time.Instant;
import java.time.LocalDate;

public record DonationDto(
        Long id,
        Long donorId,
        Long donorUserId,
        String donorName,
        Long bloodRequestId,
        String patientName,
        String hospital,
        String receiverName,
        Long donationCenterId,
        String donationCenterName,
        LocalDate donationDate,
        int units,
        int confirmedUnits,
        String notes,
        DonationStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}

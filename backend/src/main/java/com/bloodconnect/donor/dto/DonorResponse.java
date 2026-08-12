package com.bloodconnect.donor.dto;

import com.bloodconnect.common.enums.AvailabilityStatus;
import com.bloodconnect.common.enums.BloodType;

import java.time.LocalDate;

public record DonorResponse(
        Long id,
        BloodType bloodType,
        Long provinceId,
        String provinceName,
        Long municipalityId,
        String municipalityName,
        AvailabilityStatus availability,
        LocalDate lastDonationDate,
        Double approximateDistanceKm
) {
}

package com.bloodconnect.donor.dto;

import com.bloodconnect.common.enums.AvailabilityStatus;
import com.bloodconnect.common.enums.BloodType;
import com.bloodconnect.common.enums.Sex;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record DonorPrivateResponse(
        Long id,
        Long userId,
        String firstName,
        String lastName,
        String email,
        BloodType bloodType,
        LocalDate birthDate,
        Sex sex,
        String phone,
        Long provinceId,
        String provinceName,
        Long municipalityId,
        String municipalityName,
        String sector,
        String approximateAddress,
        BigDecimal latitude,
        BigDecimal longitude,
        LocalDate lastDonationDate,
        AvailabilityStatus availability,
        Instant createdAt,
        Instant updatedAt
) {
}

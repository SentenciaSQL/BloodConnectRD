package com.bloodconnect.donor.dto;

import com.bloodconnect.common.enums.BloodType;

import java.util.List;

public record BloodCompatibilityResponse(
        BloodType bloodType,
        List<BloodType> canDonateTo,
        List<BloodType> canReceiveFrom,
        String disclaimer
) {
}

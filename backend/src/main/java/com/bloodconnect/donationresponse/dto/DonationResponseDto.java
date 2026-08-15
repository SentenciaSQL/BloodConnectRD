package com.bloodconnect.donationresponse.dto;

import com.bloodconnect.common.enums.BloodType;
import com.bloodconnect.common.enums.RequestStatus;
import com.bloodconnect.common.enums.ResponseStatus;
import com.bloodconnect.common.enums.Urgency;

import java.time.Instant;

public record DonationResponseDto(
        Long id,
        Long bloodRequestId,
        Long donorId,
        Long donorUserId,
        String donorName,
        String donorPhone,
        BloodType donorBloodType,
        ResponseStatus status,
        String message,
        Instant createdAt,
        Instant updatedAt,
        String hospital,
        BloodType requestBloodType,
        String municipalityName,
        String provinceName,
        Urgency urgency,
        RequestStatus requestStatus
) {
}

package com.bloodconnect.bloodrequest.dto;

import com.bloodconnect.common.enums.BloodType;
import com.bloodconnect.common.enums.RequestStatus;
import com.bloodconnect.common.enums.Urgency;

import java.math.BigDecimal;
import java.time.Instant;

public record BloodRequestResponse(
        Long id,
        Long createdById,
        String createdByName,
        String patientName,
        BloodType bloodType,
        int unitsRequired,
        long completedUnits,
        long pendingUnits,
        int progressPercent,
        double progress,
        String hospital,
        Long provinceId,
        String provinceName,
        Long municipalityId,
        String municipalityName,
        String sector,
        String address,
        String reference,
        BigDecimal latitude,
        BigDecimal longitude,
        Instant deadline,
        String description,
        String contactPhone,
        Urgency urgency,
        RequestStatus status,
        Double approximateDistanceKm,
        Instant createdAt,
        Instant updatedAt
) {
}

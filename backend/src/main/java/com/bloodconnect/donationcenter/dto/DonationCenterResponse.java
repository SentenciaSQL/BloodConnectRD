package com.bloodconnect.donationcenter.dto;

import com.bloodconnect.common.enums.CenterType;

import java.math.BigDecimal;
import java.time.Instant;

public record DonationCenterResponse(
        Long id,
        String name,
        CenterType type,
        Long provinceId,
        String provinceName,
        Long municipalityId,
        String municipalityName,
        String sector,
        String address,
        String reference,
        String phone,
        String schedule,
        BigDecimal latitude,
        BigDecimal longitude,
        boolean active,
        Double approximateDistanceKm,
        Instant createdAt,
        Instant updatedAt
) {
}

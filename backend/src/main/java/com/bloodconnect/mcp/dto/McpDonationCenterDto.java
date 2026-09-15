package com.bloodconnect.mcp.dto;

import com.bloodconnect.common.enums.CenterType;

import java.math.BigDecimal;

/**
 * Datos públicos de un centro de donación. El teléfono es de la institución, no de una persona.
 */
public record McpDonationCenterDto(
        Long id,
        String name,
        CenterType type,
        Long provinceId,
        String provinceName,
        Long municipalityId,
        String municipalityName,
        String sector,
        String publicAddress,
        String schedule,
        String publicPhone,
        BigDecimal latitude,
        BigDecimal longitude,
        Double approximateDistanceKm
) {
}

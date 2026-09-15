package com.bloodconnect.mcp.dto;

import com.bloodconnect.common.enums.BloodType;
import com.bloodconnect.common.enums.RequestStatus;
import com.bloodconnect.common.enums.Urgency;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Vista pública y minimizada de una solicitud de sangre para consumidores MCP.
 * No incluye paciente, contacto, dirección residencial, documentos ni tokens.
 */
public record McpBloodRequestDto(
        Long id,
        BloodType bloodType,
        int unitsRequired,
        long completedUnits,
        long pendingUnits,
        int progressPercent,
        String hospital,
        Long provinceId,
        String provinceName,
        Long municipalityId,
        String municipalityName,
        String sector,
        BigDecimal latitude,
        BigDecimal longitude,
        Instant deadline,
        Urgency urgency,
        RequestStatus status,
        Instant createdAt
) {
}

package com.bloodconnect.mcp.mapper;

import com.bloodconnect.bloodrequest.dto.BloodRequestResponse;
import com.bloodconnect.common.enums.BloodType;
import com.bloodconnect.common.enums.RequestStatus;
import com.bloodconnect.common.enums.Urgency;
import com.bloodconnect.mcp.dto.McpBloodRequestDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class McpBloodRequestMapperTest {

    private final McpBloodRequestMapper mapper = new McpBloodRequestMapper();

    @Test
    void mapsPublicFieldsAndDropsPersonalData() {
        BloodRequestResponse source = new BloodRequestResponse(
                8L, 44L, "Ana Donor", "Juan Paciente", BloodType.A_POSITIVE, 2, 0, 2, 0, 0.0,
                "Hospital Cabral y Báez", 3L, "Santiago", 9L, "Santiago de los Caballeros",
                "Centro", "Residencia privada 45", "Detrás de la casa", BigDecimal.ONE, BigDecimal.TEN,
                Instant.parse("2026-09-20T12:00:00Z"), "Notas clínicas", "+18095550000",
                Urgency.CRITICAL, RequestStatus.IN_PROGRESS, 3.4, Instant.parse("2026-09-15T12:00:00Z"),
                Instant.parse("2026-09-15T13:00:00Z")
        );

        McpBloodRequestDto dto = mapper.toPublicDto(source);

        assertThat(dto.id()).isEqualTo(8L);
        assertThat(dto.hospital()).isEqualTo("Hospital Cabral y Báez");
        assertThat(dto.bloodType()).isEqualTo(BloodType.A_POSITIVE);
        assertThat(dto.sector()).isEqualTo("Centro");
        assertThat(McpBloodRequestDto.class.getRecordComponents())
                .extracting(component -> component.getName())
                .doesNotContain("patientName", "contactPhone", "address", "email", "token", "createdByName");
    }
}

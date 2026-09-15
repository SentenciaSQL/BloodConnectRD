package com.bloodconnect.mcp.service;

import com.bloodconnect.bloodrequest.dto.BloodRequestResponse;
import com.bloodconnect.bloodrequest.service.BloodRequestService;
import com.bloodconnect.common.dto.PageResponse;
import com.bloodconnect.common.enums.BloodType;
import com.bloodconnect.common.enums.CenterType;
import com.bloodconnect.common.enums.RequestStatus;
import com.bloodconnect.common.enums.Urgency;
import com.bloodconnect.donationcenter.dto.DonationCenterResponse;
import com.bloodconnect.donationcenter.service.DonationCenterService;
import com.bloodconnect.donor.service.BloodCompatibilityService;
import com.bloodconnect.exception.BadRequestException;
import com.bloodconnect.exception.ResourceNotFoundException;
import com.bloodconnect.mcp.config.McpProperties;
import com.bloodconnect.mcp.dto.McpBloodCompatibilityDto;
import com.bloodconnect.mcp.dto.McpBloodRequestDto;
import com.bloodconnect.mcp.dto.McpDonationCenterDto;
import com.bloodconnect.mcp.mapper.McpBloodRequestMapper;
import com.bloodconnect.mcp.mapper.McpDonationCenterMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class McpQueryServiceTest {

    @Mock
    private BloodRequestService bloodRequestService;
    @Mock
    private DonationCenterService donationCenterService;

    private McpQueryService mcpQueryService;

    @BeforeEach
    void setUp() {
        McpProperties properties = new McpProperties(
                true,
                "bloodconnectrd-mcp",
                "1.0.0",
                "/mcp",
                50,
                20,
                25,
                100,
                new McpProperties.RateLimit(true, 60)
        );
        mcpQueryService = new McpQueryService(
                bloodRequestService,
                donationCenterService,
                new BloodCompatibilityService(),
                new McpBloodRequestMapper(),
                new McpDonationCenterMapper(),
                properties
        );
    }

    @Test
    void listActiveBloodRequestsReusesServiceAndOmitsSensitiveFields() {
        BloodRequestResponse source = activeRequest(11L);
        when(bloodRequestService.listActive(eq(BloodType.O_POSITIVE), eq(1L), eq(2L), eq(Urgency.HIGH),
                eq("hospital"), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(source), 0, 20, 1, 1, true, true));

        var page = mcpQueryService.listActiveBloodRequests("O+", 1L, 2L, "HIGH", "hospital", 0, 20);

        assertThat(page.content()).hasSize(1);
        McpBloodRequestDto dto = page.content().get(0);
        assertThat(dto.id()).isEqualTo(11L);
        assertThat(dto.hospital()).isEqualTo("Hospital General");
        assertThat(dto.bloodType()).isEqualTo(BloodType.O_POSITIVE);
        assertThat(dto).hasNoNullFieldsOrPropertiesExcept("sector", "latitude", "longitude");
        verify(bloodRequestService).listActive(eq(BloodType.O_POSITIVE), eq(1L), eq(2L), eq(Urgency.HIGH),
                eq("hospital"), any(Pageable.class));
    }

    @Test
    void listActiveBloodRequestsRejectsSizeAboveMaximum() {
        assertThatThrownBy(() -> mcpQueryService.listActiveBloodRequests(null, null, null, null, null, 0, 51))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("50");
        verify(bloodRequestService, never()).listActive(any(), any(), any(), any(), any(), any());
    }

    @Test
    void getBloodRequestHidesExpiredCancelledAndMissing() {
        when(bloodRequestService.get(1L)).thenReturn(expiredOpenRequest());
        when(bloodRequestService.get(2L)).thenReturn(cancelledRequest());
        when(bloodRequestService.get(3L)).thenThrow(new ResourceNotFoundException("No se encontró la solicitud de sangre"));
        when(bloodRequestService.get(4L)).thenReturn(activeRequest(4L));

        assertThatThrownBy(() -> mcpQueryService.getBloodRequest(1L))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> mcpQueryService.getBloodRequest(2L))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> mcpQueryService.getBloodRequest(3L))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(mcpQueryService.getBloodRequest(4L).id()).isEqualTo(4L);
    }

    @Test
    void findNearbyDonationCentersUsesExistingService() {
        when(donationCenterService.nearby(18.48, -69.93, 25)).thenReturn(List.of(center()));

        List<McpDonationCenterDto> result = mcpQueryService.findNearbyDonationCenters(18.48, -69.93, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Cruz Roja");
        assertThat(result.get(0).publicAddress()).isEqualTo("Av. Independencia 1");
        assertThat(result.get(0).schedule()).isEqualTo("Lun-Vie 8:00-16:00");
        verify(donationCenterService).nearby(18.48, -69.93, 25);
    }

    @Test
    void findNearbyDonationCentersRejectsInvalidCoordinates() {
        assertThatThrownBy(() -> mcpQueryService.findNearbyDonationCenters(120.0, -69.93, 10.0))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("coordenadas");
        verify(donationCenterService, never()).nearby(org.mockito.ArgumentMatchers.anyDouble(),
                org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyDouble());
    }

    @Test
    void findNearbyDonationCentersRejectsRadiusOutOfBounds() {
        assertThatThrownBy(() -> mcpQueryService.findNearbyDonationCenters(18.48, -69.93, 250.0))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("100");
        verify(donationCenterService, never()).nearby(org.mockito.ArgumentMatchers.anyDouble(),
                org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyDouble());
    }

    @Test
    void checkBloodCompatibilityReusesExistingRules() {
        McpBloodCompatibilityDto compatible = mcpQueryService.checkBloodCompatibility("O-", "AB+");
        assertThat(compatible.compatible()).isTrue();
        assertThat(compatible.explanation()).contains("O-").contains("AB+");
        assertThat(compatible.disclaimer()).contains("orientativa");
        assertThat(compatible.disclaimer()).contains("centro médico");

        McpBloodCompatibilityDto incompatible = mcpQueryService.checkBloodCompatibility("AB+", "O-");
        assertThat(incompatible.compatible()).isFalse();
    }

    @Test
    void checkBloodCompatibilityRejectsInvalidType() {
        assertThatThrownBy(() -> mcpQueryService.checkBloodCompatibility("Z+", "A+"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Tipo sanguíneo no válido");
    }

    @Test
    void listActiveBloodRequestsRejectsInvalidBloodType() {
        assertThatThrownBy(() -> mcpQueryService.listActiveBloodRequests("ZZ", null, null, null, null, 0, 10))
                .isInstanceOf(BadRequestException.class);
        verify(bloodRequestService, never()).listActive(any(), any(), any(), any(), any(), any());
    }

    private BloodRequestResponse activeRequest(Long id) {
        return new BloodRequestResponse(
                id,
                99L,
                "Laura User",
                "Paciente Sensible",
                BloodType.O_POSITIVE,
                3,
                1,
                2,
                33,
                0.33,
                "Hospital General",
                1L,
                "Distrito Nacional",
                2L,
                "Santo Domingo de Guzmán",
                "Gazcue",
                "Calle residencial 12",
                "Casa al lado del colmado",
                BigDecimal.valueOf(18.47),
                BigDecimal.valueOf(-69.90),
                Instant.now().plus(2, ChronoUnit.DAYS),
                "Diagnóstico privado",
                "+18095551212",
                Urgency.HIGH,
                RequestStatus.OPEN,
                null,
                Instant.now(),
                Instant.now()
        );
    }

    private BloodRequestResponse expiredOpenRequest() {
        BloodRequestResponse active = activeRequest(1L);
        return new BloodRequestResponse(
                active.id(), active.createdById(), active.createdByName(), active.patientName(),
                active.bloodType(), active.unitsRequired(), active.completedUnits(), active.pendingUnits(),
                active.progressPercent(), active.progress(), active.hospital(), active.provinceId(),
                active.provinceName(), active.municipalityId(), active.municipalityName(), active.sector(),
                active.address(), active.reference(), active.latitude(), active.longitude(),
                Instant.now().minus(1, ChronoUnit.HOURS), active.description(), active.contactPhone(),
                active.urgency(), RequestStatus.OPEN, null, active.createdAt(), active.updatedAt()
        );
    }

    private BloodRequestResponse cancelledRequest() {
        BloodRequestResponse active = activeRequest(2L);
        return new BloodRequestResponse(
                active.id(), active.createdById(), active.createdByName(), active.patientName(),
                active.bloodType(), active.unitsRequired(), active.completedUnits(), active.pendingUnits(),
                active.progressPercent(), active.progress(), active.hospital(), active.provinceId(),
                active.provinceName(), active.municipalityId(), active.municipalityName(), active.sector(),
                active.address(), active.reference(), active.latitude(), active.longitude(),
                active.deadline(), active.description(), active.contactPhone(),
                active.urgency(), RequestStatus.CANCELLED, null, active.createdAt(), active.updatedAt()
        );
    }

    private DonationCenterResponse center() {
        return new DonationCenterResponse(
                5L,
                "Cruz Roja",
                CenterType.BLOOD_BANK,
                1L,
                "Distrito Nacional",
                2L,
                "Santo Domingo de Guzmán",
                "Gazcue",
                "Av. Independencia 1",
                null,
                "8095550000",
                "Lun-Vie 8:00-16:00",
                BigDecimal.valueOf(18.47),
                BigDecimal.valueOf(-69.90),
                true,
                1.2,
                Instant.now(),
                Instant.now()
        );
    }
}

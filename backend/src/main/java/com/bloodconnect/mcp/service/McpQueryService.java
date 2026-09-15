package com.bloodconnect.mcp.service;

import com.bloodconnect.bloodrequest.dto.BloodRequestResponse;
import com.bloodconnect.bloodrequest.service.BloodRequestService;
import com.bloodconnect.common.dto.PageResponse;
import com.bloodconnect.common.enums.BloodType;
import com.bloodconnect.common.enums.RequestStatus;
import com.bloodconnect.common.enums.Urgency;
import com.bloodconnect.common.util.PageableUtils;
import com.bloodconnect.donationcenter.dto.DonationCenterResponse;
import com.bloodconnect.donationcenter.service.DonationCenterService;
import com.bloodconnect.donor.service.BloodCompatibilityService;
import com.bloodconnect.exception.BadRequestException;
import com.bloodconnect.exception.ResourceNotFoundException;
import com.bloodconnect.mcp.config.McpProperties;
import com.bloodconnect.mcp.dto.McpBloodCompatibilityDto;
import com.bloodconnect.mcp.dto.McpBloodRequestDto;
import com.bloodconnect.mcp.dto.McpBloodRequestPageDto;
import com.bloodconnect.mcp.dto.McpDonationCenterDto;
import com.bloodconnect.mcp.mapper.McpBloodRequestMapper;
import com.bloodconnect.mcp.mapper.McpDonationCenterMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class McpQueryService {

    private static final Set<String> REQUEST_SORTS = Set.of("createdAt", "deadline", "urgency", "bloodType");
    private static final Set<RequestStatus> HIDDEN_STATUSES = Set.of(
            RequestStatus.CANCELLED,
            RequestStatus.FULFILLED,
            RequestStatus.EXPIRED
    );

    static final String COMPATIBILITY_DISCLAIMER =
            BloodCompatibilityService.DISCLAIMER
                    + " Esta información es orientativa; la compatibilidad definitiva debe confirmarla un centro médico.";

    private final BloodRequestService bloodRequestService;
    private final DonationCenterService donationCenterService;
    private final BloodCompatibilityService bloodCompatibilityService;
    private final McpBloodRequestMapper bloodRequestMapper;
    private final McpDonationCenterMapper donationCenterMapper;
    private final McpProperties mcpProperties;

    public McpBloodRequestPageDto listActiveBloodRequests(
            String bloodType,
            Long provinceId,
            Long municipalityId,
            String urgency,
            String search,
            Integer page,
            Integer size
    ) {
        Pageable pageable = createPageable(page, size);
        PageResponse<BloodRequestResponse> result = bloodRequestService.listActive(
                parseOptionalBloodType(bloodType),
                provinceId,
                municipalityId,
                parseOptionalUrgency(urgency),
                sanitizeSearch(search),
                pageable
        );
        return bloodRequestMapper.toPublicPage(result);
    }

    public McpBloodRequestDto getBloodRequest(Long requestId) {
        if (requestId == null || requestId <= 0) {
            throw new BadRequestException("El identificador de la solicitud es obligatorio");
        }
        BloodRequestResponse response = bloodRequestService.get(requestId);
        if (!isVisible(response)) {
            throw new ResourceNotFoundException("No se encontró la solicitud de sangre");
        }
        return bloodRequestMapper.toPublicDto(response);
    }

    public List<McpDonationCenterDto> findNearbyDonationCenters(
            Double latitude,
            Double longitude,
            Double radiusKm
    ) {
        if (latitude == null || longitude == null) {
            throw new BadRequestException("Debe indicar latitud y longitud");
        }
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new BadRequestException("Las coordenadas indicadas no son válidas");
        }
        double radius = radiusKm == null ? mcpProperties.defaultRadiusKm() : radiusKm;
        validateRadius(radius);
        return donationCenterService.nearby(latitude, longitude, radius).stream()
                .map(donationCenterMapper::toPublicDto)
                .toList();
    }

    public McpBloodCompatibilityDto checkBloodCompatibility(String donorBloodType, String recipientBloodType) {
        BloodType donor = parseRequiredBloodType(donorBloodType, "donorBloodType");
        BloodType recipient = parseRequiredBloodType(recipientBloodType, "recipientBloodType");
        boolean compatible = bloodCompatibilityService.canDonate(donor, recipient);
        String explanation = compatible
                ? "Según las reglas ABO y Rh que ya usa BloodConnect RD, el tipo "
                + donor.getLabel() + " puede donar al tipo " + recipient.getLabel() + "."
                : "Según las reglas ABO y Rh que ya usa BloodConnect RD, el tipo "
                + donor.getLabel() + " no puede donar al tipo " + recipient.getLabel() + ".";
        return new McpBloodCompatibilityDto(
                donor,
                recipient,
                compatible,
                explanation,
                COMPATIBILITY_DISCLAIMER
        );
    }

    private boolean isVisible(BloodRequestResponse response) {
        if (response == null || HIDDEN_STATUSES.contains(response.status())) {
            return false;
        }
        return response.deadline() != null && response.deadline().isAfter(Instant.now());
    }

    private Pageable createPageable(Integer page, Integer size) {
        int resolvedPage = page == null ? 0 : page;
        int resolvedSize = size == null ? mcpProperties.defaultPageSize() : size;
        int maxSize = Math.min(100, Math.max(1, mcpProperties.maxPageSize()));
        if (resolvedSize > maxSize) {
            throw new BadRequestException("El tamaño de página no puede exceder " + maxSize);
        }
        return PageableUtils.create(resolvedPage, resolvedSize, "createdAt", "desc", REQUEST_SORTS, "createdAt");
    }

    private void validateRadius(double radiusKm) {
        double maxRadius = mcpProperties.maxRadiusKm();
        if (radiusKm <= 0 || radiusKm > maxRadius) {
            throw new BadRequestException(
                    "El radio debe ser mayor que cero y no exceder " + trimNumber(maxRadius) + " km"
            );
        }
    }

    private BloodType parseOptionalBloodType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return parseRequiredBloodType(value, "bloodType");
    }

    private BloodType parseRequiredBloodType(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("El tipo sanguíneo " + fieldName + " es obligatorio");
        }
        try {
            return BloodType.fromLabel(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Tipo sanguíneo no válido: " + value.trim());
        }
    }

    private Urgency parseOptionalUrgency(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Urgency.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("La urgencia indicada no es válida");
        }
    }

    private String sanitizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String trimmed = search.trim();
        return trimmed.length() > 120 ? trimmed.substring(0, 120) : trimmed;
    }

    private String trimNumber(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }
}

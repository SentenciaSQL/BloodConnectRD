package com.bloodconnect.mcp.tools;

import com.bloodconnect.exception.BadRequestException;
import com.bloodconnect.exception.ResourceNotFoundException;
import com.bloodconnect.mcp.audit.McpAuditService;
import com.bloodconnect.mcp.dto.McpBloodCompatibilityDto;
import com.bloodconnect.mcp.dto.McpBloodRequestDto;
import com.bloodconnect.mcp.dto.McpBloodRequestPageDto;
import com.bloodconnect.mcp.dto.McpDonationCenterDto;
import com.bloodconnect.mcp.service.McpQueryService;
import lombok.RequiredArgsConstructor;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "bloodconnect.mcp", name = "enabled", havingValue = "true")
public class BloodConnectMcpTools {

    private final McpQueryService mcpQueryService;
    private final McpAuditService mcpAuditService;

    @McpTool(
            name = "list_active_blood_requests",
            description = "Lista solicitudes de sangre activas, abiertas y no vencidas en República Dominicana. "
                    + "No incluye datos personales sensibles.",
            annotations = @McpTool.McpAnnotations(
                    title = "Listar solicitudes activas",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false
            )
    )
    public McpBloodRequestPageDto listActiveBloodRequests(
            @McpToolParam(description = "Tipo sanguíneo (por ejemplo A+ u O-)", required = false) String bloodType,
            @McpToolParam(description = "Identificador de provincia", required = false) Long provinceId,
            @McpToolParam(description = "Identificador de municipio", required = false) Long municipalityId,
            @McpToolParam(description = "Urgencia: LOW, MEDIUM, HIGH o CRITICAL", required = false) String urgency,
            @McpToolParam(description = "Texto para buscar hospital o sector", required = false) String search,
            @McpToolParam(description = "Número de página (0-indexado)", required = false) Integer page,
            @McpToolParam(description = "Tamaño de página (máximo configurable, por defecto 50)", required = false)
            Integer size
    ) {
        return invoke("list_active_blood_requests", () -> mcpQueryService.listActiveBloodRequests(
                bloodType,
                provinceId,
                municipalityId,
                urgency,
                search,
                page,
                size
        ));
    }

    @McpTool(
            name = "get_blood_request",
            description = "Obtiene una solicitud de sangre visible (activa y no vencida) por identificador, "
                    + "sin datos personales sensibles.",
            annotations = @McpTool.McpAnnotations(
                    title = "Ver solicitud de sangre",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false
            )
    )
    public McpBloodRequestDto getBloodRequest(
            @McpToolParam(description = "Identificador de la solicitud", required = true) Long requestId
    ) {
        return invoke("get_blood_request", () -> mcpQueryService.getBloodRequest(requestId));
    }

    @McpTool(
            name = "find_nearby_donation_centers",
            description = "Busca centros de donación activos cercanos a una coordenada. "
                    + "Devuelve nombre, tipo, ubicación pública, horario y datos públicos.",
            annotations = @McpTool.McpAnnotations(
                    title = "Centros de donación cercanos",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false
            )
    )
    public List<McpDonationCenterDto> findNearbyDonationCenters(
            @McpToolParam(description = "Latitud WGS84", required = true) Double latitude,
            @McpToolParam(description = "Longitud WGS84", required = true) Double longitude,
            @McpToolParam(description = "Radio en kilómetros (predeterminado 25)", required = false) Double radiusKm
    ) {
        return invoke("find_nearby_donation_centers",
                () -> mcpQueryService.findNearbyDonationCenters(latitude, longitude, radiusKm));
    }

    @McpTool(
            name = "check_blood_compatibility",
            description = "Indica si un tipo de sangre donante es compatible con un tipo receptor "
                    + "según las reglas ABO/Rh ya usadas por BloodConnect RD. Información orientativa.",
            annotations = @McpTool.McpAnnotations(
                    title = "Compatibilidad sanguínea",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false
            )
    )
    public McpBloodCompatibilityDto checkBloodCompatibility(
            @McpToolParam(description = "Tipo sanguíneo del donante (por ejemplo O-)", required = true)
            String donorBloodType,
            @McpToolParam(description = "Tipo sanguíneo del receptor (por ejemplo A+)", required = true)
            String recipientBloodType
    ) {
        return invoke("check_blood_compatibility",
                () -> mcpQueryService.checkBloodCompatibility(donorBloodType, recipientBloodType));
    }

    private <T> T invoke(String toolName, Supplier<T> action) {
        try {
            T result = action.get();
            mcpAuditService.recordSuccess(toolName);
            return result;
        } catch (BadRequestException | ResourceNotFoundException exception) {
            mcpAuditService.recordError(toolName, exception.getClass().getSimpleName());
            throw exception;
        } catch (RuntimeException exception) {
            mcpAuditService.recordError(toolName, exception.getClass().getSimpleName());
            throw exception;
        }
    }
}

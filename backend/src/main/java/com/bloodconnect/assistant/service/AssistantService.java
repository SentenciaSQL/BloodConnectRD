package com.bloodconnect.assistant.service;

import com.bloodconnect.assistant.config.AssistantProperties;
import com.bloodconnect.assistant.dto.AssistantAskRequest;
import com.bloodconnect.assistant.dto.AssistantAskResponse;
import com.bloodconnect.assistant.dto.AssistantLinkDto;
import com.bloodconnect.assistant.intent.AssistantIntentParser;
import com.bloodconnect.assistant.intent.AssistantIntentType;
import com.bloodconnect.assistant.intent.ParsedAssistantIntent;
import com.bloodconnect.common.enums.BloodType;
import com.bloodconnect.common.enums.Urgency;
import com.bloodconnect.exception.BadRequestException;
import com.bloodconnect.exception.ResourceNotFoundException;
import com.bloodconnect.mcp.audit.McpAuditService;
import com.bloodconnect.mcp.dto.McpBloodCompatibilityDto;
import com.bloodconnect.mcp.dto.McpBloodRequestDto;
import com.bloodconnect.mcp.dto.McpBloodRequestPageDto;
import com.bloodconnect.mcp.dto.McpDonationCenterDto;
import com.bloodconnect.mcp.service.McpQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AssistantService {

    static final String HELP_REPLY =
            "Puedo consultar la misma información de solo lectura que el servidor MCP: "
                    + "solicitudes activas de sangre, el detalle público de una solicitud, "
                    + "centros de donación cercanos y compatibilidad ABO/Rh.\n\n"
                    + "Ejemplos:\n"
                    + "• ¿O- puede donar a A+?\n"
                    + "• Solicitudes de O+ urgentes\n"
                    + "• Centros cerca de mí\n"
                    + "• Solicitud 12\n\n"
                    + "No creo ni modifico solicitudes. La elegibilidad para donar la confirma un centro de salud.";

    private final AssistantProperties assistantProperties;
    private final McpQueryService mcpQueryService;
    private final McpAuditService mcpAuditService;

    public AssistantAskResponse ask(AssistantAskRequest request) {
        String message = request.message() == null ? "" : request.message().trim();
        if (message.isEmpty()) {
            throw new BadRequestException("El mensaje no puede estar vacío");
        }
        if (message.length() > assistantProperties.maxMessageLength()) {
            throw new BadRequestException(
                    "El mensaje no puede exceder " + assistantProperties.maxMessageLength() + " caracteres"
            );
        }
        ParsedAssistantIntent intent = AssistantIntentParser.parse(message);
        return dispatch(intent, request);
    }

    private AssistantAskResponse dispatch(ParsedAssistantIntent intent, AssistantAskRequest request) {
        return switch (intent.type()) {
            case HELP -> reply(HELP_REPLY, null, List.of());
            case COMPATIBILITY -> compatibility(intent);
            case LIST_REQUESTS -> listRequests(intent);
            case GET_REQUEST -> getRequest(intent);
            case NEARBY_CENTERS -> nearbyCenters(request);
            case UNKNOWN -> reply(
                    "No entendí la pregunta. Prueba con compatibilidad (¿O- puede donar a A+?), "
                            + "solicitudes activas o centros cercanos.",
                    null,
                    List.of()
            );
        };
    }

    private AssistantAskResponse compatibility(ParsedAssistantIntent intent) {
        if (intent.donorBloodType() == null || intent.recipientBloodType() == null) {
            return reply(
                    "Para revisar compatibilidad indícame el tipo del donante y el del receptor. "
                            + "Ejemplo: ¿O- puede donar a A+?",
                    null,
                    List.of(new AssistantLinkDto("Ver guía de compatibilidad", "/compatibilidad"))
            );
        }
        return invoke("check_blood_compatibility", () -> {
            McpBloodCompatibilityDto result = mcpQueryService.checkBloodCompatibility(
                    intent.donorBloodType(),
                    intent.recipientBloodType()
            );
            String reply = result.explanation() + "\n\n" + result.disclaimer();
            return reply(
                    reply,
                    "check_blood_compatibility",
                    List.of(new AssistantLinkDto("Ver guía de compatibilidad", "/compatibilidad"))
            );
        });
    }

    private AssistantAskResponse listRequests(ParsedAssistantIntent intent) {
        return invoke("list_active_blood_requests", () -> {
            McpBloodRequestPageDto page = mcpQueryService.listActiveBloodRequests(
                    intent.bloodType(),
                    null,
                    null,
                    intent.urgency(),
                    intent.search(),
                    0,
                    5
            );
            if (page.content() == null || page.content().isEmpty()) {
                return reply(
                        "No hay solicitudes activas con esos filtros. Puedes ampliar la búsqueda en el listado público.",
                        "list_active_blood_requests",
                        List.of(new AssistantLinkDto("Ver solicitudes", "/solicitudes"))
                );
            }
            StringBuilder body = new StringBuilder();
            body.append("Encontré ").append(page.totalElements()).append(" solicitud(es) activa(s)");
            if (intent.bloodType() != null) {
                body.append(" de tipo ").append(intent.bloodType());
            }
            body.append(". No incluyo nombres de pacientes ni teléfonos.\n");
            for (McpBloodRequestDto item : page.content()) {
                body.append("\n• #").append(item.id())
                        .append(" ").append(bloodLabel(item.bloodType()))
                        .append(" · ").append(nullToDash(item.hospital()))
                        .append(" · ").append(nullToDash(item.municipalityName()))
                        .append(" · urgencia ").append(urgencyLabel(item.urgency()))
                        .append(" · faltan ").append(item.pendingUnits()).append(" unidad(es)");
            }
            List<AssistantLinkDto> links = new ArrayList<>();
            links.add(new AssistantLinkDto("Ver solicitudes", "/solicitudes"));
            page.content().stream().limit(3).forEach(item ->
                    links.add(new AssistantLinkDto("Solicitud #" + item.id(), "/solicitudes/" + item.id()))
            );
            return reply(body.toString(), "list_active_blood_requests", links);
        });
    }

    private AssistantAskResponse getRequest(ParsedAssistantIntent intent) {
        return invoke("get_blood_request", () -> {
            try {
                McpBloodRequestDto item = mcpQueryService.getBloodRequest(intent.requestId());
                String reply = "Solicitud #" + item.id()
                        + " · tipo " + bloodLabel(item.bloodType())
                        + " · " + nullToDash(item.hospital())
                        + " (" + nullToDash(item.municipalityName()) + ")"
                        + " · urgencia " + urgencyLabel(item.urgency())
                        + " · " + item.pendingUnits() + " unidad(es) pendientes.\n\n"
                        + "No se muestran datos personales del paciente ni teléfonos de contacto.";
                return reply(
                        reply,
                        "get_blood_request",
                        List.of(new AssistantLinkDto("Abrir solicitud", "/solicitudes/" + item.id()))
                );
            } catch (ResourceNotFoundException exception) {
                return reply(
                        "No encontré una solicitud activa con ese identificador.",
                        "get_blood_request",
                        List.of(new AssistantLinkDto("Ver solicitudes", "/solicitudes"))
                );
            }
        });
    }

    private AssistantAskResponse nearbyCenters(AssistantAskRequest request) {
        if (request.latitude() == null || request.longitude() == null) {
            return reply(
                    "Para buscar centros cercanos necesito tu ubicación. Activa “Incluir mi ubicación” "
                            + "en el asistente, o abre el mapa de centros.",
                    null,
                    List.of(new AssistantLinkDto("Ver centros de donación", "/centros"))
            );
        }
        return invoke("find_nearby_donation_centers", () -> {
            List<McpDonationCenterDto> centers = mcpQueryService.findNearbyDonationCenters(
                    request.latitude(),
                    request.longitude(),
                    null
            );
            if (centers.isEmpty()) {
                return reply(
                        "No encontré centros de donación en ese radio. Prueba el mapa público.",
                        "find_nearby_donation_centers",
                        List.of(new AssistantLinkDto("Ver centros de donación", "/centros"))
                );
            }
            StringBuilder body = new StringBuilder("Centros de donación cercanos (datos institucionales):\n");
            centers.stream().limit(5).forEach(center -> {
                body.append("\n• ").append(nullToDash(center.name()));
                if (center.approximateDistanceKm() != null) {
                    body.append(" · ").append(String.format(Locale.ROOT, "%.1f", center.approximateDistanceKm()))
                            .append(" km");
                }
                if (center.municipalityName() != null) {
                    body.append(" · ").append(center.municipalityName());
                }
                if (center.publicPhone() != null && !center.publicPhone().isBlank()) {
                    body.append(" · ").append(center.publicPhone());
                }
            });
            return reply(
                    body.toString(),
                    "find_nearby_donation_centers",
                    List.of(new AssistantLinkDto("Ver centros de donación", "/centros"))
            );
        });
    }

    private AssistantAskResponse invoke(String toolName, SupplierWithTool supplier) {
        try {
            AssistantAskResponse response = supplier.get();
            mcpAuditService.recordSuccess(toolName);
            return response;
        } catch (BadRequestException exception) {
            mcpAuditService.recordError(toolName, "BAD_REQUEST");
            return reply(exception.getMessage(), toolName, List.of());
        } catch (RuntimeException exception) {
            mcpAuditService.recordError(toolName, exception.getClass().getSimpleName());
            throw exception;
        }
    }

    private AssistantAskResponse reply(String text, String toolUsed, List<AssistantLinkDto> links) {
        return new AssistantAskResponse(text, toolUsed, links);
    }

    private String bloodLabel(BloodType bloodType) {
        return bloodType == null ? "—" : bloodType.getLabel();
    }

    private String urgencyLabel(Urgency urgency) {
        if (urgency == null) {
            return "—";
        }
        return switch (urgency) {
            case LOW -> "baja";
            case MEDIUM -> "media";
            case HIGH -> "alta";
            case CRITICAL -> "crítica";
        };
    }

    private String nullToDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    @FunctionalInterface
    private interface SupplierWithTool {
        AssistantAskResponse get();
    }
}

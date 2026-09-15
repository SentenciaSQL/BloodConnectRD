package com.bloodconnect.mcp.tools;

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
import com.bloodconnect.mcp.audit.McpAuditService;
import com.bloodconnect.mcp.config.McpProperties;
import com.bloodconnect.mcp.mapper.McpBloodRequestMapper;
import com.bloodconnect.mcp.mapper.McpDonationCenterMapper;
import com.bloodconnect.mcp.service.McpQueryService;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springaicommunity.mcp.provider.tool.SyncMcpToolProvider;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BloodConnectMcpToolsTest {

    @Mock
    private BloodRequestService bloodRequestService;
    @Mock
    private DonationCenterService donationCenterService;
    @Mock
    private McpAuditService mcpAuditService;

    private BloodConnectMcpTools tools;
    private List<McpServerFeatures.SyncToolSpecification> specifications;

    @BeforeEach
    void setUp() {
        McpProperties properties = new McpProperties(
                true, "bloodconnectrd-mcp", "1.0.0", "/mcp", 50, 20, 25, 100,
                new McpProperties.RateLimit(true, 60)
        );
        McpQueryService queryService = new McpQueryService(
                bloodRequestService,
                donationCenterService,
                new BloodCompatibilityService(),
                new McpBloodRequestMapper(),
                new McpDonationCenterMapper(),
                properties
        );
        tools = new BloodConnectMcpTools(queryService, mcpAuditService);
        specifications = new SyncMcpToolProvider(List.of(tools)).getToolSpecifications();
    }

    @Test
    void discoversTheFourReadOnlyTools() {
        Set<String> names = specifications.stream()
                .map(spec -> spec.tool().name())
                .collect(Collectors.toSet());
        assertThat(names).containsExactlyInAnyOrder(
                "list_active_blood_requests",
                "get_blood_request",
                "find_nearby_donation_centers",
                "check_blood_compatibility"
        );
        specifications.forEach(spec -> assertThat(spec.tool().annotations().readOnlyHint()).isTrue());
    }

    @Test
    void executesListActiveBloodRequests() {
        when(bloodRequestService.listActive(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(activeRequest()), 0, 20, 1, 1, true, true));

        McpSchema.CallToolResult result = call("list_active_blood_requests", Map.of("page", 0, "size", 20));

        assertThat(result.isError()).isFalse();
        assertThat(result.content().get(0).toString()).contains("Hospital General");
        assertThat(result.content().get(0).toString()).doesNotContain("Paciente Sensible");
        assertThat(result.content().get(0).toString()).doesNotContain("+18095551212");
    }

    @Test
    void executesGetBloodRequest() {
        when(bloodRequestService.get(4L)).thenReturn(activeRequest());
        McpSchema.CallToolResult result = call("get_blood_request", Map.of("requestId", 4));
        assertThat(result.isError()).isFalse();
        assertThat(result.content().get(0).toString()).contains("\"id\":4");
    }

    @Test
    void executesFindNearbyDonationCenters() {
        when(donationCenterService.nearby(18.48, -69.93, 25.0)).thenReturn(List.of(center()));
        McpSchema.CallToolResult result = call("find_nearby_donation_centers", Map.of(
                "latitude", 18.48,
                "longitude", -69.93
        ));
        assertThat(result.isError()).isFalse();
        assertThat(result.content().get(0).toString()).contains("Cruz Roja");
    }

    @Test
    void executesCheckBloodCompatibility() {
        McpSchema.CallToolResult result = call("check_blood_compatibility", Map.of(
                "donorBloodType", "O-",
                "recipientBloodType", "A+"
        ));
        assertThat(result.isError()).isFalse();
        assertThat(result.content().get(0).toString()).contains("true");
    }

    @Test
    void invalidBloodTypeIsReportedAsToolError() {
        McpSchema.CallToolResult result = call("check_blood_compatibility", Map.of(
                "donorBloodType", "ZZ",
                "recipientBloodType", "A+"
        ));
        assertThat(result.isError()).isTrue();
    }

    @Test
    void invalidCoordinatesAreReportedAsToolError() {
        McpSchema.CallToolResult result = call("find_nearby_donation_centers", Map.of(
                "latitude", 200,
                "longitude", -69.93
        ));
        assertThat(result.isError()).isTrue();
    }

    @Test
    void radiusOutOfBoundsIsReportedAsToolError() {
        McpSchema.CallToolResult result = call("find_nearby_donation_centers", Map.of(
                "latitude", 18.48,
                "longitude", -69.93,
                "radiusKm", 500
        ));
        assertThat(result.isError()).isTrue();
    }

    @Test
    void missingRequestIsReportedAsToolError() {
        when(bloodRequestService.get(99L)).thenThrow(
                new com.bloodconnect.exception.ResourceNotFoundException("No se encontró la solicitud de sangre")
        );
        McpSchema.CallToolResult result = call("get_blood_request", Map.of("requestId", 99));
        assertThat(result.isError()).isTrue();
    }

    private McpSchema.CallToolResult call(String name, Map<String, Object> arguments) {
        McpServerFeatures.SyncToolSpecification spec = specifications.stream()
                .filter(candidate -> name.equals(candidate.tool().name()))
                .findFirst()
                .orElseThrow();
        McpSyncServerExchange exchange = mock(McpSyncServerExchange.class);
        return spec.callHandler().apply(exchange, new McpSchema.CallToolRequest(name, arguments));
    }

    private BloodRequestResponse activeRequest() {
        return new BloodRequestResponse(
                4L, 99L, "Laura User", "Paciente Sensible", BloodType.O_POSITIVE, 3, 1, 2, 33, 0.33,
                "Hospital General", 1L, "Distrito Nacional", 2L, "Santo Domingo de Guzmán", "Gazcue",
                "Calle residencial 12", "Casa", BigDecimal.valueOf(18.47), BigDecimal.valueOf(-69.90),
                Instant.now().plus(2, ChronoUnit.DAYS), "Diagnóstico privado", "+18095551212",
                Urgency.HIGH, RequestStatus.OPEN, null, Instant.now(), Instant.now()
        );
    }

    private DonationCenterResponse center() {
        return new DonationCenterResponse(
                5L, "Cruz Roja", CenterType.BLOOD_BANK, 1L, "Distrito Nacional", 2L,
                "Santo Domingo de Guzmán", "Gazcue", "Av. Independencia 1", null, "8095550000",
                "Lun-Vie 8:00-16:00", BigDecimal.valueOf(18.47), BigDecimal.valueOf(-69.90),
                true, 1.2, Instant.now(), Instant.now()
        );
    }
}

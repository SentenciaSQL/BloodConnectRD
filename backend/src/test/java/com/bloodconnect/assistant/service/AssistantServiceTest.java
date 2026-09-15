package com.bloodconnect.assistant.service;

import com.bloodconnect.assistant.config.AssistantProperties;
import com.bloodconnect.assistant.dto.AssistantAskRequest;
import com.bloodconnect.assistant.dto.AssistantAskResponse;
import com.bloodconnect.common.enums.BloodType;
import com.bloodconnect.mcp.audit.McpAuditService;
import com.bloodconnect.mcp.dto.McpBloodCompatibilityDto;
import com.bloodconnect.mcp.service.McpQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssistantServiceTest {

    @Mock
    private McpQueryService mcpQueryService;
    @Mock
    private McpAuditService mcpAuditService;

    private AssistantService assistantService;

    @BeforeEach
    void setUp() {
        assistantService = new AssistantService(
                new AssistantProperties(true, 500),
                mcpQueryService,
                mcpAuditService
        );
    }

    @Test
    void compatibilityReusesMcpQueryAndIncludesDisclaimer() {
        when(mcpQueryService.checkBloodCompatibility("O-", "A+")).thenReturn(new McpBloodCompatibilityDto(
                BloodType.O_NEGATIVE,
                BloodType.A_POSITIVE,
                true,
                "Según las reglas ABO y Rh que ya usa BloodConnect RD, el tipo O- puede donar al tipo A+.",
                "aviso médico"
        ));

        AssistantAskResponse response = assistantService.ask(new AssistantAskRequest(
                "¿O- puede donar a A+?", null, null
        ));

        assertThat(response.toolUsed()).isEqualTo("check_blood_compatibility");
        assertThat(response.reply()).contains("O- puede donar al tipo A+");
        assertThat(response.reply()).contains("aviso médico");
        assertThat(response.reply()).doesNotContain("patientName");
        assertThat(response.links()).extracting("path").contains("/compatibilidad");
        verify(mcpAuditService).recordSuccess("check_blood_compatibility");
    }

    @Test
    void nearbyCentersWithoutCoordinatesAsksForLocation() {
        AssistantAskResponse response = assistantService.ask(new AssistantAskRequest(
                "centros cerca de mí", null, null
        ));

        assertThat(response.toolUsed()).isNull();
        assertThat(response.reply()).contains("ubicación");
        assertThat(response.links()).extracting("path").contains("/centros");
    }
}

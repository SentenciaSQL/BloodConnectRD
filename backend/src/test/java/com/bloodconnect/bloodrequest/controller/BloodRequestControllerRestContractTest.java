package com.bloodconnect.bloodrequest.controller;

import com.bloodconnect.bloodrequest.dto.BloodRequestResponse;
import com.bloodconnect.bloodrequest.service.BloodRequestService;
import com.bloodconnect.common.dto.PageResponse;
import com.bloodconnect.common.enums.BloodType;
import com.bloodconnect.common.enums.RequestStatus;
import com.bloodconnect.common.enums.Urgency;
import com.bloodconnect.security.CustomUserDetailsService;
import com.bloodconnect.security.SecurityConfig;
import com.bloodconnect.security.jwt.JwtAuthenticationFilter;
import com.bloodconnect.security.jwt.JwtProperties;
import com.bloodconnect.security.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BloodRequestController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@EnableConfigurationProperties(JwtProperties.class)
@TestPropertySource(properties = {
        "jwt.secret=TEST_SECRET_MUST_BE_AT_LEAST_THIRTY_TWO_CHARS_LONG_123456",
        "jwt.expiration-ms=900000",
        "jwt.refresh-expiration-ms=604800000",
        "cors.allowed-origins=http://localhost:4200",
        "bloodconnect.mcp.enabled=false",
        "spring.ai.mcp.server.enabled=false"
})
class BloodRequestControllerRestContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BloodRequestService bloodRequestService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void publicListEndpointRemainsAvailable() throws Exception {
        BloodRequestResponse response = new BloodRequestResponse(
                1L, 2L, "Laura User", "Paciente", BloodType.A_POSITIVE, 2, 0, 2, 0, 0.0,
                "Hospital", 1L, "Distrito Nacional", 2L, "Santo Domingo de Guzmán", "Gazcue",
                "Calle 1", null, BigDecimal.ONE, BigDecimal.TEN, Instant.parse("2026-09-20T00:00:00Z"),
                "desc", "+18095551111", Urgency.MEDIUM, RequestStatus.OPEN, null,
                Instant.parse("2026-09-15T00:00:00Z"), Instant.parse("2026-09-15T00:00:00Z")
        );
        when(bloodRequestService.list(isNull(), isNull(), isNull(), isNull(), any(), isNull(), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(response), 0, 20, 1, 1, true, true));

        mockMvc.perform(get("/api/blood-requests").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].hospital").value("Hospital"));
    }

    @Test
    void publicGetEndpointRemainsAvailable() throws Exception {
        BloodRequestResponse response = new BloodRequestResponse(
                7L, 2L, "Laura User", "Paciente", BloodType.B_NEGATIVE, 1, 0, 1, 0, 0.0,
                "Clínica", 1L, "Distrito Nacional", 2L, "Santo Domingo de Guzmán", null,
                "Calle 2", null, null, null, Instant.parse("2026-09-20T00:00:00Z"),
                null, "+18095551111", Urgency.LOW, RequestStatus.OPEN, null,
                Instant.parse("2026-09-15T00:00:00Z"), Instant.parse("2026-09-15T00:00:00Z")
        );
        when(bloodRequestService.get(7L)).thenReturn(response);

        mockMvc.perform(get("/api/blood-requests/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.contactPhone").value("+18095551111"));
    }
}

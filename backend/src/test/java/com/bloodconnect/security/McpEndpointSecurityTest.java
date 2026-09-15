package com.bloodconnect.security;

import com.bloodconnect.mcp.controller.McpStatusController;
import com.bloodconnect.mcp.config.McpProperties;
import com.bloodconnect.common.controller.SystemController;
import com.bloodconnect.security.jwt.JwtAuthenticationFilter;
import com.bloodconnect.security.jwt.JwtProperties;
import com.bloodconnect.security.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {SystemController.class, McpStatusController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@EnableConfigurationProperties({JwtProperties.class, McpProperties.class})
@TestPropertySource(properties = {
        "jwt.secret=TEST_SECRET_MUST_BE_AT_LEAST_THIRTY_TWO_CHARS_LONG_123456",
        "jwt.expiration-ms=900000",
        "jwt.refresh-expiration-ms=604800000",
        "cors.allowed-origins=http://localhost:4200",
        "bloodconnect.mcp.enabled=false",
        "spring.ai.mcp.server.enabled=false"
})
class McpEndpointSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void mcpEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType("application/json")
                        .content("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Se requiere autenticación para acceder a este recurso"));
    }

    @Test
    void mcpGetRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/mcp"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void existingPublicRestApiStillWorks() throws Exception {
        mockMvc.perform(get("/api/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("BloodConnect RD"))
                .andExpect(jsonPath("$.country").value("DO"));
    }

    @Test
    void mcpStatusIsPublicAndReportsDisabledByDefault() throws Exception {
        mockMvc.perform(get("/api/mcp/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.endpoint").value("/mcp"))
                .andExpect(jsonPath("$.protocol").value("STREAMABLE"))
                .andExpect(jsonPath("$.hint").exists());
    }
}

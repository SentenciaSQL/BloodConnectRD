package com.bloodconnect.security;

import com.bloodconnect.assistant.config.AssistantProperties;
import com.bloodconnect.assistant.controller.AssistantController;
import com.bloodconnect.assistant.service.AssistantService;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {SystemController.class, AssistantController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@EnableConfigurationProperties({JwtProperties.class, AssistantProperties.class})
@TestPropertySource(properties = {
        "jwt.secret=TEST_SECRET_MUST_BE_AT_LEAST_THIRTY_TWO_CHARS_LONG_123456",
        "jwt.expiration-ms=900000",
        "jwt.refresh-expiration-ms=604800000",
        "cors.allowed-origins=http://localhost:4200",
        "bloodconnect.assistant.enabled=false",
        "bloodconnect.mcp.enabled=false",
        "spring.ai.mcp.server.enabled=false"
})
class AssistantEndpointSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private AssistantService assistantService;

    @Test
    void assistantAskRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/assistant/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"ayuda\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void assistantStatusIsPublicAndDisabledByDefault() throws Exception {
        mockMvc.perform(get("/api/assistant/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }
}

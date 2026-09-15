package com.bloodconnect.mcp.security;

import com.bloodconnect.mcp.config.McpProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class McpProtocolHintFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void unauthenticatedGetPassesThroughSoSecurityCanReturn401() throws Exception {
        McpProtocolHintFilter filter = new McpProtocolHintFilter(properties(false));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(new MockHttpServletRequest("GET", "/mcp"), response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).isEmpty();
    }

    @Test
    void getWithoutMcpEnabledReturns503() throws Exception {
        authenticate();
        McpProtocolHintFilter filter = new McpProtocolHintFilter(properties(false));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest("GET", "/mcp"), response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentAsString()).contains("desactivado");
    }

    @Test
    void getWithoutSessionReturns405() throws Exception {
        authenticate();
        McpProtocolHintFilter filter = new McpProtocolHintFilter(properties(true));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest("GET", "/mcp"), response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(405);
        assertThat(response.getContentAsString()).contains("POST");
        assertThat(response.getContentAsString()).contains("initialize");
    }

    @Test
    void getWithSessionPassesThrough() throws Exception {
        authenticate();
        McpProtocolHintFilter filter = new McpProtocolHintFilter(properties(true));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mcp");
        request.addHeader("mcp-session-id", "session-1");

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void postIsNotIntercepted() throws Exception {
        authenticate();
        McpProtocolHintFilter filter = new McpProtocolHintFilter(properties(true));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(new MockHttpServletRequest("POST", "/mcp"), response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    private void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin@bloodconnect.do",
                        "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                )
        );
    }

    private McpProperties properties(boolean enabled) {
        return new McpProperties(
                enabled, "bloodconnectrd-mcp", "1.0.0", "/mcp", 50, 20, 25, 100,
                new McpProperties.RateLimit(true, 60)
        );
    }
}

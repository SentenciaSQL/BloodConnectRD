package com.bloodconnect.mcp.security;

import com.bloodconnect.mcp.config.McpProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class McpAcceptHeaderFilterTest {

    @Test
    void injectsJsonAndSseAcceptWhenMissing() throws Exception {
        McpAcceptHeaderFilter filter = new McpAcceptHeaderFilter(properties());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mcp");
        request.addHeader("Accept", "*/*");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        HttpServletRequest forwarded = (HttpServletRequest) chain.getRequest();
        assertThat(forwarded.getHeader("Accept"))
                .isEqualTo(McpAcceptHeaderFilter.MCP_ACCEPT);
        assertThat(McpAcceptHeaderFilter.hasRequiredAccept(forwarded.getHeader("Accept"))).isTrue();
    }

    @Test
    void keepsValidAcceptHeader() throws Exception {
        McpAcceptHeaderFilter filter = new McpAcceptHeaderFilter(properties());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mcp");
        request.addHeader("Accept", "application/json, text/event-stream");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isSameAs(request);
    }

    private McpProperties properties() {
        return new McpProperties(
                true, "bloodconnectrd-mcp", "1.0.0", "/mcp", 50, 20, 25, 100,
                new McpProperties.RateLimit(true, 60)
        );
    }
}

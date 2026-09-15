package com.bloodconnect.mcp.security;

import com.bloodconnect.mcp.config.McpProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class McpRateLimitFilterTest {

    @Test
    void rejectsExcessiveRequestsOnMcpEndpoint() throws Exception {
        McpProperties properties = new McpProperties(
                true, "bloodconnectrd-mcp", "1.0.0", "/mcp", 50, 20, 25, 100,
                new McpProperties.RateLimit(true, 2)
        );
        McpRateLimitFilter filter = new McpRateLimitFilter(properties);

        MockHttpServletResponse first = new MockHttpServletResponse();
        filter.doFilter(request(), first, new MockFilterChain());
        MockHttpServletResponse second = new MockHttpServletResponse();
        filter.doFilter(request(), second, new MockFilterChain());
        MockHttpServletResponse third = new MockHttpServletResponse();
        filter.doFilter(request(), third, new MockFilterChain());

        assertThat(first.getStatus()).isEqualTo(200);
        assertThat(second.getStatus()).isEqualTo(200);
        assertThat(third.getStatus()).isEqualTo(429);
        assertThat(third.getContentAsString()).contains("límite");
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mcp");
        request.setRemoteAddr("10.0.0.8");
        return request;
    }
}

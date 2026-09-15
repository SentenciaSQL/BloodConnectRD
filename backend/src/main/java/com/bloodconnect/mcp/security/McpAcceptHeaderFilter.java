package com.bloodconnect.mcp.security;

import com.bloodconnect.mcp.config.McpProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

/**
 * Postman e IntelliJ envian Accept wildcard por defecto. El transporte Streamable HTTP
 * de MCP exige application/json y text/event-stream a la vez; sin eso el SDK
 * responde de forma que Spring acaba convirtiendola en un 500 generico.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@ConditionalOnProperty(prefix = "bloodconnect.mcp", name = "enabled", havingValue = "true")
public class McpAcceptHeaderFilter extends OncePerRequestFilter {

    static final String MCP_ACCEPT = "application/json, text/event-stream";

    private final McpProperties mcpProperties;

    public McpAcceptHeaderFilter(McpProperties mcpProperties) {
        this.mcpProperties = mcpProperties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String endpoint = mcpProperties.endpoint() == null ? "/mcp" : mcpProperties.endpoint();
        return path == null || !(path.equals(endpoint) || path.startsWith(endpoint + "/"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String accept = request.getHeader(HttpHeaders.ACCEPT);
        if (hasRequiredAccept(accept)) {
            filterChain.doFilter(request, response);
            return;
        }
        filterChain.doFilter(new AcceptOverrideRequest(request), response);
    }

    static boolean hasRequiredAccept(String accept) {
        if (accept == null || accept.isBlank()) {
            return false;
        }
        String normalized = accept.toLowerCase(Locale.ROOT);
        return normalized.contains("application/json") && normalized.contains("text/event-stream");
    }

    private static final class AcceptOverrideRequest extends HttpServletRequestWrapper {

        private AcceptOverrideRequest(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getHeader(String name) {
            if (HttpHeaders.ACCEPT.equalsIgnoreCase(name)) {
                return MCP_ACCEPT;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (HttpHeaders.ACCEPT.equalsIgnoreCase(name)) {
                return Collections.enumeration(List.of(MCP_ACCEPT));
            }
            return super.getHeaders(name);
        }
    }
}

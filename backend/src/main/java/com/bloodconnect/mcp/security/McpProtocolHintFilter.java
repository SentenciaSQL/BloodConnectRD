package com.bloodconnect.mcp.security;

import com.bloodconnect.mcp.config.McpProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * GET /mcp no es el handshake. Sin esto, Postman e Invoke-RestMethod reciben un 500 opaco.
 * Se registra como bean (no @Component) para no romper los slices WebMvcTest.
 */
public class McpProtocolHintFilter extends OncePerRequestFilter {

    private final McpProperties mcpProperties;

    public McpProtocolHintFilter(McpProperties mcpProperties) {
        this.mcpProperties = mcpProperties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
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
        if (!isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!mcpProperties.enabled()) {
            write(
                    response,
                    HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "MCP está desactivado. En PowerShell use $env:MCP_ENABLED = \"true\" "
                            + "en la MISMA ventana y luego mvn spring-boot:run. "
                            + "Si arranca desde IntelliJ, ponga MCP_ENABLED=true en Run Configuration y reinicie."
            );
            return;
        }
        String sessionId = request.getHeader("mcp-session-id");
        if (sessionId == null || sessionId.isBlank()) {
            write(
                    response,
                    HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                    "GET /mcp es el canal SSE y requiere el header mcp-session-id. "
                            + "Para probar: POST /mcp con method initialize, "
                            + "Accept application/json, text/event-stream y Authorization Bearer."
            );
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private void write(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.ALLOW, "POST, GET, DELETE");
        String escaped = message.replace("\\", "\\\\").replace("\"", "\\\"");
        response.getWriter().write("{\"message\":\"" + escaped + "\"}");
    }
}

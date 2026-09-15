package com.bloodconnect.mcp.security;

import com.bloodconnect.mcp.config.McpProperties;
import com.bloodconnect.security.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
@ConditionalOnProperty(prefix = "bloodconnect.mcp", name = "enabled", havingValue = "true")
public class McpRateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MS = 60_000L;

    private final McpProperties mcpProperties;
    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    public McpRateLimitFilter(McpProperties mcpProperties) {
        this.mcpProperties = mcpProperties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (mcpProperties.rateLimit() == null || !mcpProperties.rateLimit().enabled()) {
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
        int limit = Math.max(1, mcpProperties.rateLimit().requestsPerMinute());
        String key = clientKey(request);
        long now = Instant.now().toEpochMilli();
        Deque<Long> timestamps = hits.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() >= WINDOW_MS) {
                timestamps.removeFirst();
            }
            if (timestamps.size() >= limit) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.getWriter().write("{\"message\":\"Se excedió el límite de solicitudes MCP\"}");
                return;
            }
            timestamps.addLast(now);
        }
        filterChain.doFilter(request, response);
    }

    private String clientKey(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return "user:" + principal.getId();
        }
        if (authentication != null && authentication.isAuthenticated() && authentication.getName() != null) {
            return "principal:" + Integer.toHexString(authentication.getName().hashCode());
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return "ip:" + forwarded.split(",")[0].trim();
        }
        return "ip:" + request.getRemoteAddr();
    }
}

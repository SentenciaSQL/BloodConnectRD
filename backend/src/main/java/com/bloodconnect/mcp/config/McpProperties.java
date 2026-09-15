package com.bloodconnect.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bloodconnect.mcp")
public record McpProperties(
        boolean enabled,
        String serverName,
        String serverVersion,
        String endpoint,
        int maxPageSize,
        int defaultPageSize,
        double defaultRadiusKm,
        double maxRadiusKm,
        RateLimit rateLimit
) {
    public record RateLimit(
            boolean enabled,
            int requestsPerMinute
    ) {
    }
}

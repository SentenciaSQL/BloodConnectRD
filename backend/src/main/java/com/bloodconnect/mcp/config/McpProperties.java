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
    public McpProperties {
        if (serverName == null || serverName.isBlank()) {
            serverName = "bloodconnectrd-mcp";
        }
        if (serverVersion == null || serverVersion.isBlank()) {
            serverVersion = "1.0.0";
        }
        if (endpoint == null || endpoint.isBlank()) {
            endpoint = "/mcp";
        }
        if (maxPageSize <= 0) {
            maxPageSize = 50;
        }
        if (defaultPageSize <= 0) {
            defaultPageSize = 20;
        }
        if (defaultRadiusKm <= 0) {
            defaultRadiusKm = 25;
        }
        if (maxRadiusKm <= 0) {
            maxRadiusKm = 100;
        }
        if (rateLimit == null) {
            rateLimit = new RateLimit(true, 60);
        }
    }

    public record RateLimit(
            boolean enabled,
            int requestsPerMinute
    ) {
    }
}

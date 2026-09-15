package com.bloodconnect.assistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bloodconnect.assistant")
public record AssistantProperties(
        boolean enabled,
        int maxMessageLength
) {
    public AssistantProperties {
        if (maxMessageLength <= 0) {
            maxMessageLength = 500;
        }
    }
}

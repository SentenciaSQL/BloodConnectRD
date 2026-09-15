package com.bloodconnect.config;

import com.bloodconnect.assistant.config.AssistantProperties;
import com.bloodconnect.mcp.config.McpProperties;
import com.bloodconnect.security.jwt.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({BloodConnectProperties.class, JwtProperties.class, McpProperties.class, AssistantProperties.class})
public class AppConfig {
}

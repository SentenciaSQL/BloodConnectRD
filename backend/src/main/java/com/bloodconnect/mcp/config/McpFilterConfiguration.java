package com.bloodconnect.mcp.config;

import com.bloodconnect.mcp.security.McpRateLimitFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpFilterConfiguration {

    /**
     * Evita el doble registro del filtro (contenedor servlet + cadena de Spring Security).
     */
    @Bean
    @ConditionalOnBean(McpRateLimitFilter.class)
    public FilterRegistrationBean<McpRateLimitFilter> mcpRateLimitRegistration(McpRateLimitFilter filter) {
        FilterRegistrationBean<McpRateLimitFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}

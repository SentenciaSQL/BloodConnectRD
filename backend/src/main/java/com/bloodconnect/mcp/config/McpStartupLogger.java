package com.bloodconnect.mcp.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class McpStartupLogger implements ApplicationRunner {

    private final McpProperties mcpProperties;
    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) {
        if (!mcpProperties.enabled()) {
            log.info("Servidor MCP deshabilitado (MCP_ENABLED=false). Ver docs/MCP.md");
            return;
        }
        boolean production = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> profile.equalsIgnoreCase("prod"));
        log.info(
                "Servidor MCP habilitado name={} version={} endpoint={} protocol=STREAMABLE",
                mcpProperties.serverName(),
                mcpProperties.serverVersion(),
                mcpProperties.endpoint()
        );
        if (production) {
            log.warn(
                    "MCP está activo en el perfil prod sin OAuth 2.1 de la especificación MCP. "
                            + "No lo exponga a Internet. Pendiente documentado en docs/MCP.md."
            );
        }
    }
}

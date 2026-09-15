package com.bloodconnect.mcp.controller;

import com.bloodconnect.mcp.config.McpProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/mcp")
@RequiredArgsConstructor
public class McpStatusController {

    private final McpProperties mcpProperties;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("enabled", mcpProperties.enabled());
        body.put("endpoint", mcpProperties.endpoint());
        body.put("protocol", "STREAMABLE");
        body.put("hint", mcpProperties.enabled()
                ? "Use POST /mcp con JSON-RPC initialize. GET /mcp es SSE y necesita mcp-session-id."
                : "Defina MCP_ENABLED=true antes de arrancar Spring Boot y reinicie el proceso.");
        return ResponseEntity.ok(body);
    }
}

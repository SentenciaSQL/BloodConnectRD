package com.bloodconnect.assistant.controller;

import com.bloodconnect.assistant.config.AssistantProperties;
import com.bloodconnect.assistant.dto.AssistantAskRequest;
import com.bloodconnect.assistant.dto.AssistantAskResponse;
import com.bloodconnect.assistant.service.AssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantService assistantService;
    private final AssistantProperties assistantProperties;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("enabled", assistantProperties.enabled());
        body.put("hint", assistantProperties.enabled()
                ? "Use POST /api/assistant/ask con un mensaje en español. El navegador no habla MCP."
                : "Defina ASSISTANT_ENABLED=true antes de arrancar Spring Boot y reinicie el proceso.");
        return ResponseEntity.ok(body);
    }

    @PostMapping("/ask")
    public ResponseEntity<?> ask(@Valid @RequestBody AssistantAskRequest request) {
        if (!assistantProperties.enabled()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                            "message",
                            "El asistente está desactivado. Defina ASSISTANT_ENABLED=true y reinicie Spring Boot."
                    ));
        }
        AssistantAskResponse response = assistantService.ask(request);
        return ResponseEntity.ok(response);
    }
}

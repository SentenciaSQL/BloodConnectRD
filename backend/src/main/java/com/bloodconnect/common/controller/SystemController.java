package com.bloodconnect.common.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Sistema", description = "Información general de BloodConnect RD")
public class SystemController {

    @GetMapping("/info")
    @Operation(summary = "Información de la plataforma")
    public ResponseEntity<Map<String, Object>> info() {
        return ResponseEntity.ok(Map.of(
                "name", "BloodConnect RD",
                "country", "DO",
                "locale", "es-DO",
                "timezone", "America/Santo_Domingo",
                "serverTime", ZonedDateTime.now(ZoneId.of("America/Santo_Domingo")).toString(),
                "message", "Plataforma de donación de sangre para República Dominicana"
        ));
    }
}

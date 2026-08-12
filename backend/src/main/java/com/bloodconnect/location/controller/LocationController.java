package com.bloodconnect.location.controller;

import com.bloodconnect.location.dto.MunicipalityResponse;
import com.bloodconnect.location.dto.ProvinceResponse;
import com.bloodconnect.location.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
@Tag(name = "Ubicaciones", description = "Catálogo de provincias y municipios dominicanos")
public class LocationController {

    private final LocationService locationService;

    @GetMapping("/provinces")
    @Operation(summary = "Listar provincias")
    public ResponseEntity<List<ProvinceResponse>> getProvinces() {
        return ResponseEntity.ok(locationService.getProvinces());
    }

    @GetMapping("/provinces/{provinceId}/municipalities")
    @Operation(summary = "Listar municipios de una provincia")
    public ResponseEntity<List<MunicipalityResponse>> getMunicipalities(@PathVariable Long provinceId) {
        return ResponseEntity.ok(locationService.getMunicipalities(provinceId));
    }
}

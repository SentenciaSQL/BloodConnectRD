package com.bloodconnect.donor.controller;

import com.bloodconnect.common.enums.BloodType;
import com.bloodconnect.donor.dto.BloodCompatibilityResponse;
import com.bloodconnect.donor.service.BloodCompatibilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/blood-compatibility")
@RequiredArgsConstructor
public class BloodCompatibilityController {

    private final BloodCompatibilityService compatibilityService;

    @GetMapping("/{bloodType}")
    public ResponseEntity<BloodCompatibilityResponse> get(@PathVariable BloodType bloodType) {
        return ResponseEntity.ok(compatibilityService.getCompatibility(bloodType));
    }
}

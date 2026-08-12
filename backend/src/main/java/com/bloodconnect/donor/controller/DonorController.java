package com.bloodconnect.donor.controller;

import com.bloodconnect.common.dto.PageResponse;
import com.bloodconnect.common.enums.AvailabilityStatus;
import com.bloodconnect.common.enums.BloodType;
import com.bloodconnect.common.util.PageableUtils;
import com.bloodconnect.donor.dto.CreateDonorRequest;
import com.bloodconnect.donor.dto.DonorPrivateResponse;
import com.bloodconnect.donor.dto.DonorResponse;
import com.bloodconnect.donor.dto.UpdateAvailabilityRequest;
import com.bloodconnect.donor.dto.UpdateDonorRequest;
import com.bloodconnect.donor.service.DonorService;
import com.bloodconnect.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/donors")
@RequiredArgsConstructor
public class DonorController {

    private static final Set<String> SORTS =
            Set.of("createdAt", "bloodType", "availability", "lastDonationDate");

    private final DonorService donorService;

    @PostMapping
    public ResponseEntity<DonorPrivateResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateDonorRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(donorService.create(principal, request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<DonorResponse>> list(
            @RequestParam(required = false) BloodType bloodType,
            @RequestParam(required = false) Long provinceId,
            @RequestParam(required = false) Long municipalityId,
            @RequestParam(required = false) AvailabilityStatus availability,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        return ResponseEntity.ok(donorService.list(
                bloodType,
                provinceId,
                municipalityId,
                availability,
                latitude,
                longitude,
                PageableUtils.create(page, size, sort, direction, SORTS, "createdAt")
        ));
    }

    @GetMapping("/compatible")
    public ResponseEntity<List<DonorResponse>> compatible(
            @RequestParam BloodType bloodType,
            @RequestParam(required = false) Long provinceId,
            @RequestParam(required = false) Long municipalityId,
            @RequestParam(defaultValue = "AVAILABLE") AvailabilityStatus availability,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) Double radiusKm
    ) {
        return ResponseEntity.ok(donorService.compatible(
                bloodType,
                provinceId,
                municipalityId,
                availability,
                latitude,
                longitude,
                radiusKm
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<DonorPrivateResponse> mine(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(donorService.getMine(principal));
    }

    @PutMapping("/me")
    public ResponseEntity<DonorPrivateResponse> updateMine(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateDonorRequest request
    ) {
        return ResponseEntity.ok(donorService.updateMine(principal, request));
    }

    @PatchMapping("/me/availability")
    public ResponseEntity<DonorPrivateResponse> updateAvailability(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateAvailabilityRequest request
    ) {
        return ResponseEntity.ok(donorService.updateAvailability(principal, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DonorResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(donorService.getPublic(id));
    }
}

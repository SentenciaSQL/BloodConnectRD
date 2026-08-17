package com.bloodconnect.bloodrequest.controller;

import com.bloodconnect.bloodrequest.dto.BloodRequestResponse;
import com.bloodconnect.bloodrequest.dto.CreateBloodRequest;
import com.bloodconnect.bloodrequest.dto.UpdateBloodRequest;
import com.bloodconnect.bloodrequest.service.BloodRequestService;
import com.bloodconnect.common.dto.PageResponse;
import com.bloodconnect.common.enums.BloodType;
import com.bloodconnect.common.enums.RequestStatus;
import com.bloodconnect.common.enums.Urgency;
import com.bloodconnect.common.util.PageableUtils;
import com.bloodconnect.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/api/blood-requests")
@RequiredArgsConstructor
public class BloodRequestController {

    private static final Set<String> SORTS =
            Set.of("createdAt", "updatedAt", "deadline", "urgency", "status", "bloodType");

    private final BloodRequestService bloodRequestService;

    @PostMapping
    public ResponseEntity<BloodRequestResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateBloodRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bloodRequestService.create(principal, request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<BloodRequestResponse>> list(
            @RequestParam(required = false) BloodType bloodType,
            @RequestParam(required = false) Long provinceId,
            @RequestParam(required = false) Long municipalityId,
            @RequestParam(required = false) Urgency urgency,
            @RequestParam(required = false) List<RequestStatus> status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        return ResponseEntity.ok(bloodRequestService.list(
                bloodType,
                provinceId,
                municipalityId,
                urgency,
                status == null ? List.of() : status,
                search,
                PageableUtils.create(page, size, sort, direction, SORTS, "createdAt")
        ));
    }

    @GetMapping("/urgent")
    public ResponseEntity<PageResponse<BloodRequestResponse>> urgent(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "urgency") String sort,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        return ResponseEntity.ok(bloodRequestService.urgent(
                PageableUtils.create(page, size, sort, direction, SORTS, "urgency")
        ));
    }

    @GetMapping("/compatible")
    public ResponseEntity<PageResponse<BloodRequestResponse>> compatible(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "urgency") String sort,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        return ResponseEntity.ok(bloodRequestService.compatible(
                principal,
                PageableUtils.create(page, size, sort, direction, SORTS, "urgency")
        ));
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<BloodRequestResponse>> nearby(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(name = "radius", defaultValue = "25") double radius
    ) {
        return ResponseEntity.ok(bloodRequestService.nearby(latitude, longitude, radius));
    }

    @GetMapping("/my")
    public ResponseEntity<PageResponse<BloodRequestResponse>> mine(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        return ResponseEntity.ok(bloodRequestService.mine(
                principal,
                PageableUtils.create(page, size, sort, direction, SORTS, "createdAt")
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BloodRequestResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(bloodRequestService.get(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BloodRequestResponse> update(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateBloodRequest request
    ) {
        return ResponseEntity.ok(bloodRequestService.update(id, principal, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BloodRequestResponse> cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(bloodRequestService.cancel(id, principal));
    }
}

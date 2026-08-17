package com.bloodconnect.donation.controller;

import com.bloodconnect.common.dto.PageResponse;
import com.bloodconnect.common.util.PageableUtils;
import com.bloodconnect.donation.dto.ConfirmDonationRequest;
import com.bloodconnect.donation.dto.DonationDto;
import com.bloodconnect.donation.dto.DonationHistoryResponse;
import com.bloodconnect.donation.dto.ReportDonationRequest;
import com.bloodconnect.donation.service.DonationService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DonationController {

    private static final Set<String> SORTS =
            Set.of("donationDate", "createdAt", "status", "units");

    private final DonationService donationService;

    @GetMapping("/donations/me")
    public ResponseEntity<DonationHistoryResponse> mine(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(donationService.mine(principal));
    }

    @GetMapping("/donations/{id}")
    public ResponseEntity<DonationDto> get(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(donationService.get(id, principal));
    }

    @GetMapping("/blood-requests/{id}/donations")
    public ResponseEntity<List<DonationDto>> listForRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(donationService.listForRequest(id, principal));
    }

    @PostMapping("/blood-requests/{id}/donations")
    public ResponseEntity<DonationDto> report(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ReportDonationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(donationService.report(id, principal, request));
    }

    @PatchMapping("/donations/{id}/confirm")
    public ResponseEntity<DonationDto> confirm(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ConfirmDonationRequest request
    ) {
        return ResponseEntity.ok(donationService.confirm(id, principal, request));
    }

    @GetMapping("/admin/donations")
    public ResponseEntity<PageResponse<DonationDto>> adminList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "donationDate") String sort,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        return ResponseEntity.ok(donationService.adminList(
                PageableUtils.create(page, size, sort, direction, SORTS, "donationDate")
        ));
    }
}

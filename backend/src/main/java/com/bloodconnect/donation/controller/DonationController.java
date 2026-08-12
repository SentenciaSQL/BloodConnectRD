package com.bloodconnect.donation.controller;

import com.bloodconnect.common.dto.PageResponse;
import com.bloodconnect.common.util.PageableUtils;
import com.bloodconnect.donation.dto.DonationDto;
import com.bloodconnect.donation.dto.DonationHistoryResponse;
import com.bloodconnect.donation.service.DonationService;
import com.bloodconnect.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

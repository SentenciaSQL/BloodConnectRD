package com.bloodconnect.donationresponse.controller;

import com.bloodconnect.donationresponse.dto.CompleteDonationResponseRequest;
import com.bloodconnect.donationresponse.dto.CreateDonationResponseRequest;
import com.bloodconnect.donationresponse.dto.DonationResponseDto;
import com.bloodconnect.donationresponse.service.DonationResponseService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DonationResponseController {

    private final DonationResponseService responseService;

    @PostMapping("/blood-requests/{id}/responses")
    public ResponseEntity<DonationResponseDto> create(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody(required = false) CreateDonationResponseRequest request
    ) {
        CreateDonationResponseRequest body =
                request == null ? new CreateDonationResponseRequest(null) : request;
        return ResponseEntity.status(HttpStatus.CREATED).body(responseService.create(id, principal, body));
    }

    @GetMapping("/blood-requests/{id}/responses")
    public ResponseEntity<List<DonationResponseDto>> list(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(responseService.listForRequest(id, principal));
    }

    @PatchMapping("/donation-responses/{id}/accept")
    public ResponseEntity<DonationResponseDto> accept(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(responseService.accept(id, principal));
    }

    @PatchMapping("/donation-responses/{id}/reject")
    public ResponseEntity<DonationResponseDto> reject(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(responseService.reject(id, principal));
    }

    @PatchMapping("/donation-responses/{id}/complete")
    public ResponseEntity<DonationResponseDto> complete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody(required = false) CompleteDonationResponseRequest request
    ) {
        int units = request == null || request.units() == null ? 1 : request.units();
        return ResponseEntity.ok(responseService.complete(id, principal, units));
    }

    @PatchMapping("/donation-responses/{id}/cancel")
    public ResponseEntity<DonationResponseDto> cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(responseService.cancel(id, principal));
    }
}

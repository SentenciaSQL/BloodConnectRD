package com.bloodconnect.donationcenter.controller;

import com.bloodconnect.auth.dto.MessageResponse;
import com.bloodconnect.common.dto.PageResponse;
import com.bloodconnect.common.enums.CenterType;
import com.bloodconnect.common.util.PageableUtils;
import com.bloodconnect.donationcenter.dto.DonationCenterRequest;
import com.bloodconnect.donationcenter.dto.DonationCenterResponse;
import com.bloodconnect.donationcenter.service.DonationCenterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api")
@RequiredArgsConstructor
public class DonationCenterController {

    private static final Set<String> SORTS = Set.of("name", "type", "createdAt");

    private final DonationCenterService centerService;

    @GetMapping("/donation-centers")
    public ResponseEntity<PageResponse<DonationCenterResponse>> list(
            @RequestParam(required = false) Long provinceId,
            @RequestParam(required = false) Long municipalityId,
            @RequestParam(required = false) CenterType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        return ResponseEntity.ok(centerService.list(
                provinceId,
                municipalityId,
                type,
                PageableUtils.create(page, size, sort, direction, SORTS, "name")
        ));
    }

    @GetMapping("/donation-centers/nearby")
    public ResponseEntity<List<DonationCenterResponse>> nearby(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(name = "radius", defaultValue = "25") double radius
    ) {
        return ResponseEntity.ok(centerService.nearby(latitude, longitude, radius));
    }

    @GetMapping("/donation-centers/{id}")
    public ResponseEntity<DonationCenterResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(centerService.getPublic(id));
    }

    @PostMapping("/admin/donation-centers")
    public ResponseEntity<DonationCenterResponse> create(
            @Valid @RequestBody DonationCenterRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(centerService.create(request));
    }

    @PutMapping("/admin/donation-centers/{id}")
    public ResponseEntity<DonationCenterResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody DonationCenterRequest request
    ) {
        return ResponseEntity.ok(centerService.update(id, request));
    }

    @DeleteMapping("/admin/donation-centers/{id}")
    public ResponseEntity<MessageResponse> delete(@PathVariable Long id) {
        return ResponseEntity.ok(centerService.delete(id));
    }
}

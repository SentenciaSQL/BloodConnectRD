package com.bloodconnect.device.controller;

import com.bloodconnect.auth.dto.MessageResponse;
import com.bloodconnect.device.dto.DeviceTokenResponse;
import com.bloodconnect.device.dto.RegisterDeviceRequest;
import com.bloodconnect.device.service.DeviceService;
import com.bloodconnect.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @PostMapping
    public ResponseEntity<DeviceTokenResponse> register(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RegisterDeviceRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(deviceService.register(principal, request));
    }

    @DeleteMapping("/{token}")
    public ResponseEntity<MessageResponse> delete(
            @PathVariable String token,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(deviceService.delete(principal, token));
    }
}

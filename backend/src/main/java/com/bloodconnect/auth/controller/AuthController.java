package com.bloodconnect.auth.controller;

import com.bloodconnect.auth.dto.AuthResponse;
import com.bloodconnect.auth.dto.LoginRequest;
import com.bloodconnect.auth.dto.MessageResponse;
import com.bloodconnect.auth.dto.RefreshTokenRequest;
import com.bloodconnect.auth.dto.RegisterRequest;
import com.bloodconnect.auth.service.AuthService;
import com.bloodconnect.security.UserPrincipal;
import com.bloodconnect.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Registro y gestión de sesiones")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Registrar una cuenta")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renovar el token de acceso")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión")
    public ResponseEntity<MessageResponse> logout(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody(required = false) RefreshTokenRequest request
    ) {
        return ResponseEntity.ok(authService.logout(principal, request));
    }

    @GetMapping("/me")
    @Operation(summary = "Consultar el usuario autenticado")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(authService.me(principal));
    }
}

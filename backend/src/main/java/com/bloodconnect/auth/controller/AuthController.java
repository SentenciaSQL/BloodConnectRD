package com.bloodconnect.auth.controller;

import com.bloodconnect.auth.dto.AuthResponse;
import com.bloodconnect.auth.dto.LoginRequest;
import com.bloodconnect.auth.dto.MessageResponse;
import com.bloodconnect.auth.dto.RefreshTokenRequest;
import com.bloodconnect.auth.dto.RegisterRequest;
import com.bloodconnect.auth.service.AuthService;
import com.bloodconnect.security.UserPrincipal;
import com.bloodconnect.user.dto.UserResponse;
import com.bloodconnect.auth.dto.ForgotPasswordRequest;
import com.bloodconnect.auth.dto.ResetPasswordRequest;
import com.bloodconnect.auth.dto.VerifyEmailRequest;
import com.bloodconnect.auth.dto.ResendVerificationRequest;
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
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest request) {
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

    @PostMapping("/forgot-password")
    @Operation(summary = "Solicitar recuperación de contraseña")
    public ResponseEntity<MessageResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        return ResponseEntity.ok(
                authService.forgotPassword(request)
        );
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Restablecer la contraseña")
    public ResponseEntity<MessageResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        return ResponseEntity.ok(
                authService.resetPassword(request)
        );
    }

    @GetMapping("/me")
    @Operation(summary = "Consultar el usuario autenticado")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(authService.me(principal));
    }

    @PostMapping("/verify-email")
    @Operation(
            summary = "Verificar correo electrónico"
    )
    public ResponseEntity<MessageResponse> verifyEmail(
            @Valid
            @RequestBody
            VerifyEmailRequest request
    ) {
        return ResponseEntity.ok(
                authService.verifyEmail(request)
        );
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Reenviar correo de verificación")
    public ResponseEntity<MessageResponse> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        return ResponseEntity.ok(
                authService.resendVerification(request)
        );
    }
}

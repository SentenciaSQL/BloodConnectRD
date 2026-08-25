package com.bloodconnect.auth.service;

import com.bloodconnect.auth.dto.AuthResponse;
import com.bloodconnect.auth.dto.LoginRequest;
import com.bloodconnect.auth.dto.MessageResponse;
import com.bloodconnect.auth.dto.RefreshTokenRequest;
import com.bloodconnect.auth.dto.RegisterRequest;
import com.bloodconnect.auth.dto.ForgotPasswordRequest;
import com.bloodconnect.auth.dto.ResetPasswordRequest;
import com.bloodconnect.auth.entity.RefreshToken;
import com.bloodconnect.common.enums.Role;
import com.bloodconnect.common.util.PhoneNormalizer;
import com.bloodconnect.exception.BadRequestException;
import com.bloodconnect.exception.ConflictException;
import com.bloodconnect.exception.ResourceNotFoundException;
import com.bloodconnect.security.UserPrincipal;
import com.bloodconnect.security.jwt.JwtProperties;
import com.bloodconnect.security.jwt.JwtService;
import com.bloodconnect.user.dto.UserResponse;
import com.bloodconnect.user.entity.User;
import com.bloodconnect.user.mapper.UserMapper;
import com.bloodconnect.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;
    private final PasswordResetService passwordResetService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new BadRequestException("Las contraseñas no coinciden");
        }

        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Ya existe una cuenta con este correo electrónico");
        }

        String phone;
        try {
            phone = PhoneNormalizer.normalize(request.phone());
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException(exception.getMessage());
        }

        User user = User.builder()
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .phone(phone)
                .role(Role.USER)
                .enabled(true)
                .build();

        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Ya existe una cuenta con este correo electrónico");
        }

        RefreshToken refreshToken = refreshTokenService.create(user);
        return createAuthResponse(user, refreshToken.getToken());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password())
            );
        } catch (AuthenticationException exception) {
            throw new BadCredentialsException("Correo electrónico o contraseña incorrectos");
        }

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = findUser(principal.getId());
        RefreshToken refreshToken = refreshTokenService.create(user);
        return createAuthResponse(user, refreshToken.getToken());
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.validate(request.refreshToken());
        User user = refreshToken.getUser();
        if (!user.isEnabled()) {
            throw new BadCredentialsException("La cuenta de usuario está deshabilitada");
        }
        return createAuthResponse(user, refreshToken.getToken());
    }

    @Transactional
    public MessageResponse logout(UserPrincipal principal, RefreshTokenRequest request) {
        User user = findUser(principal.getId());
        if (request == null) {
            refreshTokenService.revokeAllForUser(user);
        } else {
            refreshTokenService.revoke(request.refreshToken(), principal.getId());
        }
        return new MessageResponse("Sesión cerrada correctamente");
    }

    @Transactional(readOnly = true)
    public UserResponse me(UserPrincipal principal) {
        return userMapper.toResponse(findUser(principal.getId()));
    }

    @Transactional
    public MessageResponse forgotPassword(
            ForgotPasswordRequest request
    ) {
        String email = normalizeEmail(request.email());

        userRepository.findByEmail(email)
                .filter(User::isEnabled)
                .ifPresent(passwordResetService::createAndSend);

        return new MessageResponse(
                "Si existe una cuenta con ese correo, "
                        + "recibirás instrucciones para restablecer la contraseña"
        );
    }

    @Transactional
    public MessageResponse resetPassword(
            ResetPasswordRequest request
    ) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new BadRequestException(
                    "Las contraseñas no coinciden"
            );
        }

        User user = passwordResetService.consume(request.token());

        user.setPassword(
                passwordEncoder.encode(request.password())
        );

        userRepository.save(user);

        // Cierra todas las sesiones activas del usuario
        refreshTokenService.revokeAllForUser(user);

        return new MessageResponse(
                "Contraseña restablecida correctamente"
        );
    }

    private AuthResponse createAuthResponse(User user, String refreshToken) {
        UserPrincipal principal = UserPrincipal.from(user);
        String accessToken = jwtService.generateAccessToken(principal);
        long expiresIn = Math.max(0L, jwtProperties.expirationMs() / 1_000L);
        return new AuthResponse(
                accessToken,
                refreshToken,
                expiresIn,
                userMapper.toResponse(user)
        );
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el usuario autenticado"
                ));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}

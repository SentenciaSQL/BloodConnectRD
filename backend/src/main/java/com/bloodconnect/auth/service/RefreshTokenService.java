package com.bloodconnect.auth.service;

import com.bloodconnect.auth.entity.RefreshToken;
import com.bloodconnect.auth.repository.RefreshTokenRepository;
import com.bloodconnect.security.jwt.JwtProperties;
import com.bloodconnect.security.jwt.JwtService;
import com.bloodconnect.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    @Transactional
    public RefreshToken create(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(jwtService.generateRefreshTokenValue())
                .expiresAt(Instant.now().plusMillis(jwtProperties.refreshExpirationMs()))
                .revoked(false)
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional(readOnly = true)
    public RefreshToken validate(String tokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new BadCredentialsException(
                        "El token de actualización no es válido"
                ));

        if (refreshToken.isRevoked()) {
            throw new BadCredentialsException("El token de actualización fue revocado");
        }
        if (!refreshToken.getExpiresAt().isAfter(Instant.now())) {
            throw new BadCredentialsException("El token de actualización ha expirado");
        }
        return refreshToken;
    }

    @Transactional
    public void revoke(String tokenValue, Long userId) {
        refreshTokenRepository.findByToken(tokenValue)
                .filter(token -> token.getUser().getId().equals(userId))
                .ifPresent(token -> refreshTokenRepository.revokeByToken(token.getToken()));
    }

    @Transactional
    public void revokeAllForUser(User user) {
        refreshTokenRepository.revokeAllByUser(user);
    }
}

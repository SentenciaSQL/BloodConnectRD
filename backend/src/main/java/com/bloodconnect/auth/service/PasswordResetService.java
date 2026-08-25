package com.bloodconnect.auth.service;

import com.bloodconnect.auth.entity.PasswordResetToken;
import com.bloodconnect.auth.repository.PasswordResetTokenRepository;
import com.bloodconnect.exception.BadRequestException;
import com.bloodconnect.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final JavaMailSender mailSender;

    @Value("${bloodconnect.password-reset.expiration-minutes:30}")
    private long expirationMinutes;

    @Value("${bloodconnect.password-reset.url}")
    private String resetUrl;

    @Value("${spring.mail.username}")
    private String from;

    public void createAndSend(User user) {
        tokenRepository.deleteByUser(user);

        String rawToken =
                UUID.randomUUID().toString()
                        + UUID.randomUUID();

        PasswordResetToken token = PasswordResetToken.builder()
                .user(user)
                .tokenHash(hash(rawToken))
                .expiresAt(
                        Instant.now().plus(
                                expirationMinutes,
                                ChronoUnit.MINUTES
                        )
                )
                .build();

        tokenRepository.save(token);

        String link = resetUrl
                + (resetUrl.contains("?") ? "&" : "?")
                + "token="
                + rawToken;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(user.getEmail());
        message.setSubject(
                "Recupera tu contraseña de BloodConnect RD"
        );
        message.setText(
                "Hola " + user.getFirstName() + ",\n\n"
                        + "Puedes restablecer tu contraseña aquí:\n"
                        + link
                        + "\n\nEste enlace vence en "
                        + expirationMinutes
                        + " minutos."
        );

        try {
            mailSender.send(message);
            System.out.println(
                    "Correo de recuperación enviado correctamente a: "
                            + user.getEmail()
            );
        } catch (Exception exception) {
            System.err.println(
                    "ERROR ENVIANDO CORREO: "
                            + exception.getMessage()
            );
            exception.printStackTrace();
            throw exception;
        }
    }

    public User consume(String rawToken) {
        String tokenHash = hash(rawToken.trim());

        PasswordResetToken token = tokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadRequestException(
                        "El token de recuperación no es válido"
                ));

        if (token.getUsedAt() != null) {
            throw new BadRequestException(
                    "El token de recuperación ya fue utilizado"
            );
        }

        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException(
                    "El token de recuperación ha expirado"
            );
        }

        token.setUsedAt(Instant.now());
        tokenRepository.save(token);

        return token.getUser();
    }

    private String hash(String value) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] encoded = digest.digest(
                    value.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(encoded);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 no está disponible",
                    exception
            );
        }
    }
}

package com.bloodconnect.auth.service;

import com.bloodconnect.auth.entity.PasswordResetToken;
import com.bloodconnect.auth.repository.PasswordResetTokenRepository;
import com.bloodconnect.exception.BadRequestException;
import com.bloodconnect.user.entity.User;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;

    @Value("${bloodconnect.password-reset.expiration-minutes:30}")
    private long expirationMinutes;

    @Value("${bloodconnect.password-reset.url}")
    private String resetUrl;

    @Value("${bloodconnect.password-reset.mail-enabled:false}")
    private boolean mailEnabled;

    @Value("${resend.api-key}")
    private String resendApiKey;

    @Value("${bloodconnect.mail.from}")
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

        if (!mailEnabled) {
            System.out.println(
                    "Enlace de recuperación: " + link
            );
            return;
        }

        String body =
                "Hola " + user.getFirstName() + ",\n\n"
                        + "Recibimos una solicitud para restablecer "
                        + "la contraseña de tu cuenta de BloodConnect RD.\n\n"
                        + "Puedes crear una nueva contraseña utilizando "
                        + "el siguiente enlace:\n\n"
                        + link
                        + "\n\nEste enlace vence en "
                        + expirationMinutes
                        + " minutos y solo puede utilizarse una vez.\n\n"
                        + "Si no solicitaste este cambio, puedes ignorar "
                        + "este mensaje.\n\n"
                        + "Este es un mensaje automático. "
                        + "Por favor, no respondas a este correo.";

        try {
            System.out.println(
                    "Intentando enviar correo de recuperación a: "
                            + user.getEmail()
            );

            System.out.println(
                    "Remitente configurado: " + from
            );

            System.out.println(
                    "API key de Resend configurada: "
                            + (
                            resendApiKey != null
                                    && !resendApiKey.isBlank()
                    )
            );

            CreateEmailOptions email = CreateEmailOptions.builder()
                    .from(from)
                    .to(user.getEmail())
                    .subject(
                            "Recupera tu contraseña de BloodConnect RD"
                    )
                    .text(body)
                    .build();

            Resend resend = new Resend(resendApiKey);

            var response = resend.emails().send(email);

            System.out.println(
                    "Correo enviado correctamente. Resend ID: "
                            + response.getId()
            );
        } catch (Exception exception) {
            System.err.println(
                    "ERROR ENVIANDO CORREO CON RESEND: "
                            + exception.getMessage()
            );

            exception.printStackTrace();

            throw new IllegalStateException(
                    "No se pudo enviar el correo de recuperación",
                    exception
            );
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

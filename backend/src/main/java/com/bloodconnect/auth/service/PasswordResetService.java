package com.bloodconnect.auth.service;

import com.bloodconnect.auth.entity.PasswordResetToken;
import com.bloodconnect.auth.repository.PasswordResetTokenRepository;
import com.bloodconnect.exception.BadRequestException;
import com.bloodconnect.user.entity.User;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
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
import java.time.Duration;
import java.util.Map;

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

            SimpleClientHttpRequestFactory requestFactory =
                    new SimpleClientHttpRequestFactory();

            requestFactory.setConnectTimeout(
                    Duration.ofSeconds(10)
            );

            requestFactory.setReadTimeout(
                    Duration.ofSeconds(20)
            );

            RestClient resendClient = RestClient.builder()
                    .baseUrl("https://api.resend.com")
                    .requestFactory(requestFactory)
                    .defaultHeader(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + resendApiKey
                    )
                    .defaultHeader(
                            HttpHeaders.CONTENT_TYPE,
                            MediaType.APPLICATION_JSON_VALUE
                    )
                    .build();

            Map<String, Object> requestBody = Map.of(
                    "from", from,
                    "to", new String[]{user.getEmail()},
                    "subject",
                    "Recupera tu contraseña de BloodConnect RD",
                    "text", body
            );

            System.out.println(
                    "Llamando directamente a la API de Resend..."
            );

            ResendEmailResponse response = resendClient.post()
                    .uri("/emails")
                    .body(requestBody)
                    .retrieve()
                    .body(ResendEmailResponse.class);

            if (response == null || response.id() == null) {
                throw new IllegalStateException(
                        "Resend respondió sin un identificador de correo"
                );
            }

            System.out.println(
                    "Correo enviado correctamente. Resend ID: "
                            + response.id()
            );
        } catch (RestClientResponseException exception) {
            System.err.println(
                    "RESEND RESPONDIÓ CON HTTP "
                            + exception.getStatusCode()
            );

            System.err.println(
                    "RESPUESTA DE RESEND: "
                            + exception.getResponseBodyAsString()
            );

            throw new IllegalStateException(
                    "Resend rechazó el envío del correo",
                    exception
            );
        } catch (ResourceAccessException exception) {
            System.err.println(
                    "NO SE PUDO CONECTAR CON RESEND: "
                            + exception.getMessage()
            );

            throw new IllegalStateException(
                    "No se pudo conectar con la API de Resend",
                    exception
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

    private record ResendEmailResponse(String id) {
    }
}

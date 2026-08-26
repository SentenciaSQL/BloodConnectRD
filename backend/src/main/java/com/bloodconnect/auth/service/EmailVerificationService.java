package com.bloodconnect.auth.service;

import com.bloodconnect.auth.entity.EmailVerificationToken;
import com.bloodconnect.auth.repository.EmailVerificationTokenRepository;
import com.bloodconnect.exception.BadRequestException;
import com.bloodconnect.user.entity.User;
import com.bloodconnect.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final RestClient resendClient;

    @Value("${resend.api-key}")
    private String resendApiKey;

    @Value("${bloodconnect.mail.from}")
    private String from;

    @Value("${bloodconnect.email-verification.url}")
    private String verificationUrl;

    @Value("${bloodconnect.email-verification.expiration-hours:24}")
    private long expirationHours;

    @Value("${bloodconnect.email-verification.mail-enabled:false}")
    private boolean mailEnabled;

    public EmailVerificationService(
            EmailVerificationTokenRepository tokenRepository,
            UserRepository userRepository,
            RestClient.Builder restClientBuilder
    ) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;

        this.resendClient = restClientBuilder
                .baseUrl("https://api.resend.com")
                .build();
    }

    /**
     * Crea un token y envia el correo de verificacion.
     */
    @Transactional
    public void createAndSend(User user) {
        if (user.isEmailVerified()) {
            return;
        }

        tokenRepository.deleteByUser(user);

        String rawToken =
                UUID.randomUUID().toString()
                        + UUID.randomUUID();

        EmailVerificationToken token =
                EmailVerificationToken.builder()
                        .user(user)
                        .tokenHash(hash(rawToken))
                        .expiresAt(
                                Instant.now().plus(
                                        expirationHours,
                                        ChronoUnit.HOURS
                                )
                        )
                        .build();

        tokenRepository.save(token);

        String link = verificationUrl
                + (verificationUrl.contains("?") ? "&" : "?")
                + "token="
                + rawToken;

        if (!mailEnabled) {
            System.out.println(
                    "Enlace de verificacion de correo: "
                            + link
            );
            return;
        }

        validateResendConfiguration();

        String firstName =
                user.getFirstName() == null
                        || user.getFirstName().isBlank()
                        ? "usuario"
                        : user.getFirstName();

        String body =
                "Hola " + firstName + ",\n\n"
                        + "Gracias por registrarte en "
                        + "BloodConnect RD.\n\n"
                        + "Para confirmar tu direccion de correo "
                        + "electronico, abre el siguiente enlace:\n\n"
                        + link
                        + "\n\nEste enlace vence en "
                        + expirationHours
                        + " horas y solo puede utilizarse una vez.\n\n"
                        + "Si no creaste esta cuenta, puedes ignorar "
                        + "este mensaje.\n\n"
                        + "Este es un mensaje automatico. "
                        + "Por favor, no respondas a este correo.";

        sendEmail(
                user.getEmail(),
                "Confirma tu correo electronico en BloodConnect RD",
                body
        );
    }

    /**
     * Valida el token y marca el correo como verificado.
     */
    @Transactional
    public void verify(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BadRequestException(
                    "El token de verificacion es obligatorio"
            );
        }

        String tokenHash = hash(rawToken.trim());

        EmailVerificationToken token = tokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(
                        () -> new BadRequestException(
                                "El enlace de verificacion "
                                        + "no es valido"
                        )
                );

        if (token.isUsed()) {
            throw new BadRequestException(
                    "El enlace de verificacion "
                            + "ya fue utilizado"
            );
        }

        if (token.isExpired()) {
            throw new BadRequestException(
                    "El enlace de verificacion ha expirado"
            );
        }

        User user = token.getUser();

        if (!user.isEmailVerified()) {
            user.setEmailVerified(true);
            userRepository.save(user);
        }

        token.markAsUsed();
        tokenRepository.save(token);
    }

    /**
     * Genera y envia un enlace nuevo.
     */
    @Transactional
    public void resend(User user) {
        if (user.isEmailVerified()) {
            return;
        }

        createAndSend(user);
    }

    private void sendEmail(
            String destination,
            String subject,
            String body
    ) {
        Map<String, Object> requestBody = Map.of(
                "from", from,
                "to", List.of(destination),
                "subject", subject,
                "text", body
        );

        try {
            System.out.println(
                    "Intentando enviar correo de verificacion a: "
                            + destination
            );

            resendClient
                    .post()
                    .uri("/emails")
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + resendApiKey
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .toBodilessEntity();

            System.out.println(
                    "Correo de verificacion enviado correctamente"
            );
        } catch (RestClientResponseException exception) {
            System.err.println(
                    "ERROR DE RESEND: HTTP "
                            + exception.getStatusCode()
            );

            System.err.println(
                    "Respuesta de Resend: "
                            + exception.getResponseBodyAsString()
            );

            throw new IllegalStateException(
                    "No se pudo enviar el correo "
                            + "de verificacion",
                    exception
            );
        } catch (RestClientException exception) {
            System.err.println(
                    "ERROR DE CONEXION CON RESEND: "
                            + exception.getMessage()
            );

            throw new IllegalStateException(
                    "No se pudo conectar con Resend",
                    exception
            );
        }
    }

    private void validateResendConfiguration() {
        if (resendApiKey == null
                || resendApiKey.isBlank()) {
            throw new IllegalStateException(
                    "RESEND_API_KEY no esta configurada"
            );
        }

        if (from == null || from.isBlank()) {
            throw new IllegalStateException(
                    "MAIL_FROM no esta configurado"
            );
        }

        if (verificationUrl == null
                || verificationUrl.isBlank()) {
            throw new IllegalStateException(
                    "EMAIL_VERIFICATION_URL "
                            + "no esta configurada"
            );
        }
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
                    "SHA-256 no esta disponible",
                    exception
            );
        }
    }
}

package com.bloodconnect.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

@Configuration
@ConditionalOnProperty(
        name = "bloodconnect.firebase.enabled",
        havingValue = "true"
)
public class FirebaseConfig {

    @Bean
    FirebaseApp firebaseApp(
            @Value(
                    "${bloodconnect.firebase.credentials-base64}"
            )
            String credentialsBase64
    ) throws IOException {

        if (credentialsBase64 == null
                || credentialsBase64.isBlank()) {
            throw new IllegalStateException(
                    "FIREBASE_CREDENTIALS_BASE64 "
                            + "es obligatoria cuando "
                            + "FIREBASE_ENABLED=true"
            );
        }

        byte[] credentialsJson;

        try {
            credentialsJson = Base64.getDecoder().decode(
                    credentialsBase64.trim()
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "FIREBASE_CREDENTIALS_BASE64 "
                            + "no contiene un Base64 válido",
                    exception
            );
        }

        GoogleCredentials credentials =
                GoogleCredentials.fromStream(
                        new ByteArrayInputStream(
                                credentialsJson
                        )
                );

        FirebaseOptions options =
                FirebaseOptions.builder()
                        .setCredentials(credentials)
                        .build();

        if (FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.initializeApp(options);
        }

        return FirebaseApp.getInstance();
    }

    @Bean
    FirebaseMessaging firebaseMessaging(
            FirebaseApp firebaseApp
    ) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}

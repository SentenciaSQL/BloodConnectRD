package com.bloodconnect.notification.service;

import com.bloodconnect.device.entity.DeviceToken;
import com.bloodconnect.device.repository.DeviceTokenRepository;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "bloodconnect.firebase.enabled",
        havingValue = "true"
)
public class FirebasePushNotificationService
        implements PushNotificationService {

    private static final int FCM_MULTICAST_LIMIT = 500;

    private final FirebaseMessaging firebaseMessaging;
    private final DeviceTokenRepository deviceTokenRepository;

    @Override
    public void sendToUser(
            Long userId,
            String title,
            String message
    ) {
        sendToUser(
                userId,
                title,
                message,
                null,
                null
        );
    }

    @Override
    public void sendToUser(
            Long userId,
            String title,
            String message,
            String resourceType,
            Long resourceId
    ) {
        List<DeviceToken> devices =
                deviceTokenRepository.findAllByUserId(userId);

        if (devices.isEmpty()) {
            log.debug(
                    "El usuario {} no tiene dispositivos "
                            + "registrados para push",
                    userId
            );
            return;
        }

        for (
                int start = 0;
                start < devices.size();
                start += FCM_MULTICAST_LIMIT
        ) {
            int end = Math.min(
                    start + FCM_MULTICAST_LIMIT,
                    devices.size()
            );

            sendBatch(
                    userId,
                    title,
                    message,
                    resourceType,
                    resourceId,
                    devices.subList(start, end)
            );
        }
    }

    private void sendBatch(
            Long userId,
            String title,
            String body,
            String resourceType,
            Long resourceId,
            List<DeviceToken> devices
    ) {
        MulticastMessage.Builder builder =
                MulticastMessage.builder()
                        .setNotification(
                                Notification.builder()
                                        .setTitle(title)
                                        .setBody(body)
                                        .build()
                        )
                        .setAndroidConfig(
                                AndroidConfig.builder()
                                        .setPriority(
                                                AndroidConfig
                                                        .Priority
                                                        .HIGH
                                        )
                                        .build()
                        )
                        .setApnsConfig(
                                ApnsConfig.builder()
                                        .setAps(
                                                Aps.builder()
                                                        .setSound(
                                                                "default"
                                                        )
                                                        .build()
                                        )
                                        .build()
                        )
                        .addAllTokens(
                                devices.stream()
                                        .map(DeviceToken::getToken)
                                        .toList()
                        );

        if (resourceType != null
                && !resourceType.isBlank()) {
            builder.putData(
                    "resourceType",
                    resourceType
            );
        }

        if (resourceId != null) {
            builder.putData(
                    "resourceId",
                    resourceId.toString()
            );
        }

        builder.putData("title", title);
        builder.putData("body", body);

        try {
            BatchResponse response =
                    firebaseMessaging
                            .sendEachForMulticast(
                                    builder.build()
                            );

            removeInvalidTokens(
                    devices,
                    response.getResponses()
            );

            log.info(
                    "Push enviado: userId={}, "
                            + "exitosos={}, fallidos={}",
                    userId,
                    response.getSuccessCount(),
                    response.getFailureCount()
            );
        } catch (FirebaseMessagingException exception) {
            log.error(
                    "No se pudo enviar el push "
                            + "al usuario {}: {}",
                    userId,
                    exception.getMessage()
            );
        } catch (RuntimeException exception) {
            log.error(
                    "Error inesperado enviando push "
                            + "al usuario {}",
                    userId,
                    exception
            );
        }
    }

    private void removeInvalidTokens(
            List<DeviceToken> devices,
            List<SendResponse> responses
    ) {
        List<DeviceToken> invalidDevices =
                new ArrayList<>();

        for (
                int index = 0;
                index < responses.size();
                index++
        ) {
            SendResponse response = responses.get(index);

            if (response.isSuccessful()
                    || response.getException() == null) {
                continue;
            }

            MessagingErrorCode code =
                    response.getException()
                            .getMessagingErrorCode();

            if (code == MessagingErrorCode.UNREGISTERED
                    || code
                    == MessagingErrorCode.INVALID_ARGUMENT) {
                invalidDevices.add(devices.get(index));
            }
        }

        if (!invalidDevices.isEmpty()) {
            deviceTokenRepository.deleteAllInBatch(
                    invalidDevices
            );

            log.info(
                    "Se eliminaron {} tokens FCM inválidos",
                    invalidDevices.size()
            );
        }
    }
}

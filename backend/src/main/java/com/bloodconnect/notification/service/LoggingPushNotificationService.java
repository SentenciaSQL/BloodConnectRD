package com.bloodconnect.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LoggingPushNotificationService implements PushNotificationService {

    @Override
    public void sendToUser(Long userId, String title, String message) {
        log.info(
                "Notificación push pendiente de integración Firebase: userId={}, title={}, message={}",
                userId,
                title,
                message
        );
    }
}

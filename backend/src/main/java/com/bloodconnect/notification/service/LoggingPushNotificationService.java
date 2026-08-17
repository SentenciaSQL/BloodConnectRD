package com.bloodconnect.notification.service;

import com.bloodconnect.device.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoggingPushNotificationService implements PushNotificationService {

    private final DeviceTokenRepository deviceTokenRepository;

    @Override
    public void sendToUser(Long userId, String title, String message) {
        sendToUser(userId, title, message, null, null);
    }

    @Override
    public void sendToUser(Long userId, String title, String message, String resourceType, Long resourceId) {
        long devices = deviceTokenRepository.countByUserId(userId);
        log.info(
                "Push para dispositivo en segundo plano: userId={}, devices={}, title={}, message={}, resourceType={}, resourceId={}",
                userId,
                devices,
                title,
                message,
                resourceType,
                resourceId
        );
    }
}

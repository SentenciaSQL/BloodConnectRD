package com.bloodconnect.notification.service;

public interface PushNotificationService {

    void sendToUser(Long userId, String title, String message);
}

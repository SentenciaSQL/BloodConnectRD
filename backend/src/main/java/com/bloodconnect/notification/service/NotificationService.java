package com.bloodconnect.notification.service;

import com.bloodconnect.auth.dto.MessageResponse;
import com.bloodconnect.common.dto.PageResponse;
import com.bloodconnect.common.enums.NotificationType;
import com.bloodconnect.exception.ResourceNotFoundException;
import com.bloodconnect.notification.dto.NotificationResponse;
import com.bloodconnect.notification.entity.Notification;
import com.bloodconnect.notification.repository.NotificationRepository;
import com.bloodconnect.security.UserPrincipal;
import com.bloodconnect.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final PushNotificationService pushNotificationService;

    @Transactional
    public Notification create(
            User user,
            NotificationType type,
            String title,
            String message,
            String resourceType,
            Long resourceId
    ) {
        Notification notification = notificationRepository.save(Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .read(false)
                .build());
        pushNotificationService.sendToUser(user.getId(), title, message);
        return notification;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(UserPrincipal principal, Pageable pageable) {
        return PageResponse.from(notificationRepository.findByUserId(principal.getId(), pageable)
                .map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> unread(UserPrincipal principal, Pageable pageable) {
        return PageResponse.from(notificationRepository.findByUserIdAndReadFalse(principal.getId(), pageable)
                .map(this::toResponse));
    }

    @Transactional
    public NotificationResponse markRead(Long id, UserPrincipal principal) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la notificación"));
        if (!notification.getUser().getId().equals(principal.getId())) {
            throw new AccessDeniedException("No puede modificar notificaciones de otro usuario");
        }
        notification.setRead(true);
        return toResponse(notificationRepository.save(notification));
    }

    @Transactional
    public MessageResponse markAllRead(UserPrincipal principal) {
        notificationRepository.markAllRead(principal.getId());
        return new MessageResponse("Todas las notificaciones fueron marcadas como leídas");
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getResourceType(),
                notification.getResourceId(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}

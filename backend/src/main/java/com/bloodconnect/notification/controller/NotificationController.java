package com.bloodconnect.notification.controller;

import com.bloodconnect.auth.dto.MessageResponse;
import com.bloodconnect.common.dto.PageResponse;
import com.bloodconnect.common.util.PageableUtils;
import com.bloodconnect.notification.dto.NotificationResponse;
import com.bloodconnect.notification.service.NotificationService;
import com.bloodconnect.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private static final Set<String> SORTS = Set.of("createdAt", "read", "type");

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<PageResponse<NotificationResponse>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        return ResponseEntity.ok(notificationService.list(
                principal,
                PageableUtils.create(page, size, sort, direction, SORTS, "createdAt")
        ));
    }

    @GetMapping("/unread")
    public ResponseEntity<PageResponse<NotificationResponse>> unread(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(notificationService.unread(
                principal,
                PageableUtils.create(page, size, "createdAt", "desc", SORTS, "createdAt")
        ));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(notificationService.markRead(id, principal));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<MessageResponse> markAllRead(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(notificationService.markAllRead(principal));
    }
}

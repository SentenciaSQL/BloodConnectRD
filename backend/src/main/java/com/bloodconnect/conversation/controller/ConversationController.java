package com.bloodconnect.conversation.controller;

import com.bloodconnect.conversation.dto.ChatMessageDto;
import com.bloodconnect.conversation.dto.ConversationDto;
import com.bloodconnect.conversation.dto.OpenConversationRequest;
import com.bloodconnect.conversation.dto.SendMessageRequest;
import com.bloodconnect.conversation.dto.UnreadCountResponse;
import com.bloodconnect.conversation.service.ConversationService;
import com.bloodconnect.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @PostMapping("/blood-requests/{id}/conversations")
    public ResponseEntity<ConversationDto> open(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody(required = false) OpenConversationRequest request
    ) {
        return ResponseEntity.ok(conversationService.openOrGet(id, principal, request));
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationDto>> list(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(conversationService.list(principal));
    }

    @GetMapping("/conversations/{id}")
    public ResponseEntity<ConversationDto> get(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(conversationService.get(id, principal));
    }

    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<List<ChatMessageDto>> messages(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(conversationService.messages(id, principal));
    }

    @PostMapping("/conversations/{id}/messages")
    public ResponseEntity<ChatMessageDto> send(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SendMessageRequest request
    ) {
        return ResponseEntity.ok(conversationService.send(id, principal, request));
    }

    @GetMapping("/messages/unread-count")
    public ResponseEntity<UnreadCountResponse> unreadCount(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(conversationService.unreadCount(principal));
    }

    @PutMapping("/conversations/{id}/read")
    public ResponseEntity<ConversationDto> markRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(conversationService.markRead(id, principal));
    }

    @PostMapping("/conversations/{id}/read")
    public ResponseEntity<ConversationDto> markReadPost(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(conversationService.markRead(id, principal));
    }
}

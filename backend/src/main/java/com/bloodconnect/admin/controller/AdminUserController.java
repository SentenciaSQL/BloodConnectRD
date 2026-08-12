package com.bloodconnect.admin.controller;

import com.bloodconnect.admin.dto.UpdateUserStatusRequest;
import com.bloodconnect.admin.service.AdminUserService;
import com.bloodconnect.common.dto.PageResponse;
import com.bloodconnect.common.enums.Role;
import com.bloodconnect.common.util.PageableUtils;
import com.bloodconnect.security.UserPrincipal;
import com.bloodconnect.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private static final Set<String> SORTS =
            Set.of("createdAt", "firstName", "lastName", "email", "role", "enabled");

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<PageResponse<UserResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        return ResponseEntity.ok(adminUserService.list(
                search,
                role,
                enabled,
                PageableUtils.create(page, size, sort, direction, SORTS, "createdAt")
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.get(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponse> updateStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        return ResponseEntity.ok(adminUserService.updateStatus(id, request, principal));
    }
}

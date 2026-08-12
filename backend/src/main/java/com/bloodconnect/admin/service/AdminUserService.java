package com.bloodconnect.admin.service;

import com.bloodconnect.admin.dto.UpdateUserStatusRequest;
import com.bloodconnect.auth.repository.RefreshTokenRepository;
import com.bloodconnect.common.dto.PageResponse;
import com.bloodconnect.common.enums.Role;
import com.bloodconnect.exception.BadRequestException;
import com.bloodconnect.exception.ResourceNotFoundException;
import com.bloodconnect.security.UserPrincipal;
import com.bloodconnect.user.dto.UserResponse;
import com.bloodconnect.user.entity.User;
import com.bloodconnect.user.mapper.UserMapper;
import com.bloodconnect.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(
            String search,
            Role role,
            Boolean enabled,
            Pageable pageable
    ) {
        Specification<User> specification = (root, query, cb) -> cb.conjunction();
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("firstName")), pattern),
                    cb.like(cb.lower(root.get("lastName")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern),
                    cb.like(cb.lower(root.get("phone")), pattern)
            ));
        }
        if (role != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("role"), role));
        }
        if (enabled != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("enabled"), enabled));
        }
        return PageResponse.from(userRepository.findAll(specification, pageable).map(userMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public UserResponse get(Long id) {
        return userMapper.toResponse(find(id));
    }

    @Transactional
    public UserResponse updateStatus(
            Long id,
            UpdateUserStatusRequest request,
            UserPrincipal principal
    ) {
        User user = find(id);
        if (user.getId().equals(principal.getId()) && !request.enabled()) {
            throw new BadRequestException("No puede deshabilitar su propia cuenta administrativa");
        }
        user.setEnabled(request.enabled());
        if (!request.enabled()) {
            refreshTokenRepository.revokeAllByUser(user);
        }
        return userMapper.toResponse(userRepository.save(user));
    }

    private User find(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el usuario"));
    }
}

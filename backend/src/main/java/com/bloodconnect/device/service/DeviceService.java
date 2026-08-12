package com.bloodconnect.device.service;

import com.bloodconnect.auth.dto.MessageResponse;
import com.bloodconnect.device.dto.DeviceTokenResponse;
import com.bloodconnect.device.dto.RegisterDeviceRequest;
import com.bloodconnect.device.entity.DeviceToken;
import com.bloodconnect.device.repository.DeviceTokenRepository;
import com.bloodconnect.exception.ConflictException;
import com.bloodconnect.exception.ResourceNotFoundException;
import com.bloodconnect.security.UserPrincipal;
import com.bloodconnect.user.entity.User;
import com.bloodconnect.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public DeviceTokenResponse register(UserPrincipal principal, RegisterDeviceRequest request) {
        DeviceToken token = deviceTokenRepository.findByToken(request.token().trim()).orElse(null);
        if (token != null && !token.getUser().getId().equals(principal.getId())) {
            throw new ConflictException("El token del dispositivo ya está registrado por otro usuario");
        }
        if (token == null) {
            User user = userRepository.findById(principal.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "No se encontró el usuario autenticado"
                    ));
            token = DeviceToken.builder()
                    .user(user)
                    .token(request.token().trim())
                    .platform(request.platform())
                    .build();
        } else {
            token.setPlatform(request.platform());
        }
        return toResponse(deviceTokenRepository.save(token));
    }

    @Transactional
    public MessageResponse delete(UserPrincipal principal, String tokenValue) {
        DeviceToken token = deviceTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el dispositivo registrado"
                ));
        if (!token.getUser().getId().equals(principal.getId())) {
            throw new AccessDeniedException("No puede eliminar dispositivos de otro usuario");
        }
        deviceTokenRepository.delete(token);
        return new MessageResponse("Dispositivo eliminado correctamente");
    }

    private DeviceTokenResponse toResponse(DeviceToken token) {
        return new DeviceTokenResponse(
                token.getId(),
                token.getToken(),
                token.getPlatform(),
                token.getCreatedAt(),
                token.getUpdatedAt()
        );
    }
}

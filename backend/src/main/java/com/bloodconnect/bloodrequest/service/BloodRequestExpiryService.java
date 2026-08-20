package com.bloodconnect.bloodrequest.service;

import com.bloodconnect.bloodrequest.repository.BloodRequestRepository;
import com.bloodconnect.common.enums.RequestStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Cierra automáticamente las solicitudes cuya fecha límite ya pasó.
 * Sin este proceso, una solicitud OPEN/IN_PROGRESS vencida sigue apareciendo
 * como activa indefinidamente, porque nada más transiciona su estado a EXPIRED.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BloodRequestExpiryService {

    private static final List<RequestStatus> EXPIRABLE_STATUSES =
            List.of(RequestStatus.OPEN, RequestStatus.IN_PROGRESS);

    private final BloodRequestRepository bloodRequestRepository;

    @Scheduled(
            initialDelayString = "${bloodconnect.request-expiry.initial-delay-ms:0}",
            fixedRateString = "${bloodconnect.request-expiry.fixed-rate-ms:300000}"
    )
    @Transactional
    public void expireOverdueRequests() {
        int updated = bloodRequestRepository.expireOverdue(
                EXPIRABLE_STATUSES,
                RequestStatus.EXPIRED,
                Instant.now()
        );
        if (updated > 0) {
            log.info("Se marcaron {} solicitudes como EXPIRED por haber superado su fecha límite", updated);
        }
    }
}

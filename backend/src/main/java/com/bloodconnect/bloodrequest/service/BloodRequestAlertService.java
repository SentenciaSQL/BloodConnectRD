package com.bloodconnect.bloodrequest.service;

import com.bloodconnect.bloodrequest.entity.BloodRequest;
import com.bloodconnect.common.enums.AvailabilityStatus;
import com.bloodconnect.common.enums.BloodType;
import com.bloodconnect.common.enums.NotificationType;
import com.bloodconnect.common.enums.Urgency;
import com.bloodconnect.common.util.GeoUtils;
import com.bloodconnect.donor.entity.Donor;
import com.bloodconnect.donor.repository.DonorRepository;
import com.bloodconnect.donor.service.BloodCompatibilityService;
import com.bloodconnect.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Notifica a donantes disponibles cuando se publica una solicitud compatible.
 * No revela datos sensibles del paciente.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BloodRequestAlertService {

    private static final double NEARBY_RADIUS_KM = 25.0;
    private static final int MAX_DONORS_TO_NOTIFY = 200;

    private final DonorRepository donorRepository;
    private final BloodCompatibilityService compatibilityService;
    private final NotificationService notificationService;

    @Transactional
    public void notifyCompatibleDonors(BloodRequest request) {
        List<BloodType> compatibleDonorTypes = compatibilityService.canReceiveFrom(request.getBloodType());
        if (compatibleDonorTypes.isEmpty()) {
            return;
        }

        List<Donor> donors = donorRepository.findByAvailabilityAndBloodTypeIn(
                AvailabilityStatus.AVAILABLE,
                compatibleDonorTypes
        );

        Long creatorUserId = request.getCreatedBy().getId();
        boolean urgent = request.getUrgency() == Urgency.HIGH || request.getUrgency() == Urgency.CRITICAL;
        String municipality = request.getMunicipality().getName();
        String bloodLabel = request.getBloodType().getLabel();

        Set<Long> notifiedUsers = new HashSet<>();
        int sent = 0;

        for (Donor donor : donors) {
            if (sent >= MAX_DONORS_TO_NOTIFY) {
                break;
            }
            Long donorUserId = donor.getUser().getId();
            if (donorUserId.equals(creatorUserId) || !notifiedUsers.add(donorUserId)) {
                continue;
            }

            boolean nearby = isNearby(request, donor);
            NotificationType type;
            String title;
            String message;

            if (urgent) {
                type = NotificationType.URGENT_REQUEST;
                title = "Solicitud urgente compatible";
                message = "Se necesita sangre " + bloodLabel + " urgentemente en " + municipality + ".";
            } else if (nearby) {
                type = NotificationType.NEARBY_REQUEST;
                title = "Solicitud compatible cerca de ti";
                message = "Hay una solicitud compatible de sangre " + bloodLabel
                        + " aproximadamente cerca de tu ubicación en " + municipality + ".";
            } else {
                type = NotificationType.COMPATIBLE_REQUEST;
                title = "Nueva solicitud compatible";
                message = "Se necesita sangre " + bloodLabel + " en " + municipality + ".";
            }

            notificationService.create(
                    donor.getUser(),
                    type,
                    title,
                    message,
                    "BLOOD_REQUEST",
                    request.getId()
            );
            sent++;
        }

        log.info(
                "Solicitud {} notificada a {} donantes disponibles (tipo {}, urgencia {})",
                request.getId(),
                sent,
                bloodLabel,
                request.getUrgency()
        );
    }

    private boolean isNearby(BloodRequest request, Donor donor) {
        if (request.getLatitude() == null
                || request.getLongitude() == null
                || donor.getLatitude() == null
                || donor.getLongitude() == null) {
            return false;
        }
        double distance = GeoUtils.haversineKm(
                request.getLatitude().doubleValue(),
                request.getLongitude().doubleValue(),
                donor.getLatitude().doubleValue(),
                donor.getLongitude().doubleValue()
        );
        return distance <= NEARBY_RADIUS_KM;
    }
}

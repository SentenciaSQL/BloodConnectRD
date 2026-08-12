package com.bloodconnect.donor.service;

import com.bloodconnect.common.enums.BloodType;
import com.bloodconnect.donor.dto.BloodCompatibilityResponse;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class BloodCompatibilityService {

    public static final String DISCLAIMER =
            "La compatibilidad sanguínea mostrada es informativa. "
                    + "La elegibilidad para donar debe ser determinada por profesionales de la salud.";

    private static final Map<BloodType, List<BloodType>> DONATE_TO = new EnumMap<>(BloodType.class);

    static {
        DONATE_TO.put(BloodType.O_NEGATIVE, List.of(BloodType.values()));
        DONATE_TO.put(BloodType.O_POSITIVE, List.of(
                BloodType.O_POSITIVE, BloodType.A_POSITIVE, BloodType.B_POSITIVE, BloodType.AB_POSITIVE
        ));
        DONATE_TO.put(BloodType.A_NEGATIVE, List.of(
                BloodType.A_NEGATIVE, BloodType.A_POSITIVE, BloodType.AB_NEGATIVE, BloodType.AB_POSITIVE
        ));
        DONATE_TO.put(BloodType.A_POSITIVE, List.of(BloodType.A_POSITIVE, BloodType.AB_POSITIVE));
        DONATE_TO.put(BloodType.B_NEGATIVE, List.of(
                BloodType.B_NEGATIVE, BloodType.B_POSITIVE, BloodType.AB_NEGATIVE, BloodType.AB_POSITIVE
        ));
        DONATE_TO.put(BloodType.B_POSITIVE, List.of(BloodType.B_POSITIVE, BloodType.AB_POSITIVE));
        DONATE_TO.put(BloodType.AB_NEGATIVE, List.of(BloodType.AB_NEGATIVE, BloodType.AB_POSITIVE));
        DONATE_TO.put(BloodType.AB_POSITIVE, List.of(BloodType.AB_POSITIVE));
    }

    public boolean canDonate(BloodType donorType, BloodType recipientType) {
        return DONATE_TO.get(donorType).contains(recipientType);
    }

    public List<BloodType> canDonateTo(BloodType bloodType) {
        return DONATE_TO.get(bloodType);
    }

    public List<BloodType> canReceiveFrom(BloodType bloodType) {
        return List.of(BloodType.values()).stream()
                .filter(donorType -> canDonate(donorType, bloodType))
                .toList();
    }

    public BloodCompatibilityResponse getCompatibility(BloodType bloodType) {
        return new BloodCompatibilityResponse(
                bloodType,
                canDonateTo(bloodType),
                canReceiveFrom(bloodType),
                DISCLAIMER
        );
    }
}

package com.bloodconnect.donor.dto;

import com.bloodconnect.common.enums.AvailabilityStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAvailabilityRequest(
        @NotNull(message = "La disponibilidad es obligatoria") AvailabilityStatus availability
) {
}

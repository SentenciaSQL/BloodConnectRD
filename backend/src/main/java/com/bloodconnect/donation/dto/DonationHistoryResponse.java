package com.bloodconnect.donation.dto;

import java.time.LocalDate;
import java.util.List;

public record DonationHistoryResponse(
        long totalDonations,
        long totalUnits,
        LocalDate lastDonation,
        LocalDate estimatedNextDate,
        String orientationNote,
        List<DonationDto> history
) {
}

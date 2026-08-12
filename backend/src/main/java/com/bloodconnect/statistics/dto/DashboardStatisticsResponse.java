package com.bloodconnect.statistics.dto;

import java.util.Map;

public record DashboardStatisticsResponse(
        long users,
        long donors,
        long availableDonors,
        long openRequests,
        long fulfilledRequests,
        long donations,
        Map<String, Long> bloodTypeDistribution,
        Map<String, Long> requestsByProvince,
        Map<String, Long> requestsByMunicipality,
        Map<String, Long> donationsByMonth
) {
}

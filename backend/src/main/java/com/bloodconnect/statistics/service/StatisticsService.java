package com.bloodconnect.statistics.service;

import com.bloodconnect.bloodrequest.entity.BloodRequest;
import com.bloodconnect.bloodrequest.repository.BloodRequestRepository;
import com.bloodconnect.common.enums.AvailabilityStatus;
import com.bloodconnect.common.enums.DonationStatus;
import com.bloodconnect.common.enums.RequestStatus;
import com.bloodconnect.donation.entity.Donation;
import com.bloodconnect.donation.repository.DonationRepository;
import com.bloodconnect.donor.entity.Donor;
import com.bloodconnect.donor.repository.DonorRepository;
import com.bloodconnect.statistics.dto.DashboardStatisticsResponse;
import com.bloodconnect.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final UserRepository userRepository;
    private final DonorRepository donorRepository;
    private final BloodRequestRepository bloodRequestRepository;
    private final DonationRepository donationRepository;

    @Transactional(readOnly = true)
    public DashboardStatisticsResponse dashboard() {
        var donors = donorRepository.findAll();
        var requests = bloodRequestRepository.findAll();
        var donations = donationRepository.findAll();

        Map<String, Long> bloodTypes = donors.stream()
                .collect(Collectors.groupingBy(
                        donor -> donor.getBloodType().getLabel(),
                        TreeMap::new,
                        Collectors.counting()
                ));
        Map<String, Long> byProvince = groupRequests(
                requests,
                request -> request.getProvince().getName()
        );
        Map<String, Long> byMunicipality = groupRequests(
                requests,
                request -> request.getMunicipality().getName()
        );
        Map<String, Long> byMonth = donations.stream()
                .filter(donation -> donation.getStatus() == DonationStatus.COMPLETED)
                .collect(Collectors.groupingBy(
                        donation -> donation.getDonationDate().format(MONTH_FORMAT),
                        TreeMap::new,
                        Collectors.counting()
                ));

        return new DashboardStatisticsResponse(
                userRepository.count(),
                donors.size(),
                donorRepository.countByAvailability(AvailabilityStatus.AVAILABLE),
                bloodRequestRepository.countByStatus(RequestStatus.OPEN),
                bloodRequestRepository.countByStatus(RequestStatus.FULFILLED),
                donations.stream().filter(d -> d.getStatus() == DonationStatus.COMPLETED).count(),
                new LinkedHashMap<>(bloodTypes),
                byProvince,
                byMunicipality,
                new LinkedHashMap<>(byMonth)
        );
    }

    private Map<String, Long> groupRequests(
            Iterable<BloodRequest> requests,
            Function<BloodRequest, String> classifier
    ) {
        Map<String, Long> result = new TreeMap<>();
        for (BloodRequest request : requests) {
            result.merge(classifier.apply(request), 1L, Long::sum);
        }
        return new LinkedHashMap<>(result);
    }
}

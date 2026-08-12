package com.bloodconnect.donation.service;

import com.bloodconnect.common.dto.PageResponse;
import com.bloodconnect.common.enums.DonationStatus;
import com.bloodconnect.common.enums.Role;
import com.bloodconnect.donation.dto.DonationDto;
import com.bloodconnect.donation.dto.DonationHistoryResponse;
import com.bloodconnect.donation.entity.Donation;
import com.bloodconnect.donation.repository.DonationRepository;
import com.bloodconnect.donor.entity.Donor;
import com.bloodconnect.donor.service.DonorService;
import com.bloodconnect.exception.ResourceNotFoundException;
import com.bloodconnect.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DonationService {

    public static final String ORIENTATION_NOTE =
            "La fecha estimada se calcula a 56 días de la última donación y es solo orientativa. "
                    + "La elegibilidad debe ser determinada por profesionales de la salud.";

    private final DonationRepository donationRepository;
    private final DonorService donorService;

    @Transactional(readOnly = true)
    public DonationHistoryResponse mine(UserPrincipal principal) {
        Donor donor = donorService.findByUserId(principal.getId());
        List<Donation> donations = donationRepository.findByDonorIdOrderByDonationDateDesc(donor.getId());
        List<Donation> completed = donations.stream()
                .filter(donation -> donation.getStatus() == DonationStatus.COMPLETED)
                .toList();
        LocalDate lastDonation = completed.stream()
                .map(Donation::getDonationDate)
                .max(LocalDate::compareTo)
                .orElse(donor.getLastDonationDate());
        return new DonationHistoryResponse(
                completed.size(),
                completed.stream().mapToLong(Donation::getUnits).sum(),
                lastDonation,
                lastDonation == null ? null : lastDonation.plusDays(56),
                ORIENTATION_NOTE,
                donations.stream().map(this::toDto).toList()
        );
    }

    @Transactional(readOnly = true)
    public DonationDto get(Long id, UserPrincipal principal) {
        Donation donation = donationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la donación"));
        if (principal.getRole() != Role.ADMIN
                && !donation.getDonor().getUser().getId().equals(principal.getId())) {
            throw new AccessDeniedException("No puede consultar donaciones de otro usuario");
        }
        return toDto(donation);
    }

    @Transactional(readOnly = true)
    public PageResponse<DonationDto> adminList(Pageable pageable) {
        return PageResponse.from(donationRepository.findAll(pageable).map(this::toDto));
    }

    private DonationDto toDto(Donation donation) {
        return new DonationDto(
                donation.getId(),
                donation.getDonor().getId(),
                donation.getDonor().getUser().getFirstName() + " "
                        + donation.getDonor().getUser().getLastName(),
                donation.getBloodRequest() == null ? null : donation.getBloodRequest().getId(),
                donation.getDonationCenter() == null ? null : donation.getDonationCenter().getId(),
                donation.getDonationCenter() == null ? null : donation.getDonationCenter().getName(),
                donation.getDonationDate(),
                donation.getUnits(),
                donation.getNotes(),
                donation.getStatus(),
                donation.getCreatedAt()
        );
    }
}

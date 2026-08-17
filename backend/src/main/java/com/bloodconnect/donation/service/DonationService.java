package com.bloodconnect.donation.service;

import com.bloodconnect.bloodrequest.dto.DonationProgress;
import com.bloodconnect.bloodrequest.entity.BloodRequest;
import com.bloodconnect.bloodrequest.repository.BloodRequestRepository;
import com.bloodconnect.bloodrequest.service.BloodRequestService;
import com.bloodconnect.common.dto.PageResponse;
import com.bloodconnect.common.enums.DonationStatus;
import com.bloodconnect.common.enums.NotificationType;
import com.bloodconnect.common.enums.RequestStatus;
import com.bloodconnect.common.enums.ResponseStatus;
import com.bloodconnect.common.enums.Role;
import com.bloodconnect.donation.dto.ConfirmDonationRequest;
import com.bloodconnect.donation.dto.DonationDto;
import com.bloodconnect.donation.dto.DonationHistoryResponse;
import com.bloodconnect.donation.dto.ReportDonationRequest;
import com.bloodconnect.donation.entity.Donation;
import com.bloodconnect.donation.repository.DonationRepository;
import com.bloodconnect.donationresponse.entity.DonationResponse;
import com.bloodconnect.donationresponse.repository.DonationResponseRepository;
import com.bloodconnect.donor.entity.Donor;
import com.bloodconnect.donor.repository.DonorRepository;
import com.bloodconnect.donor.service.DonorService;
import com.bloodconnect.exception.BadRequestException;
import com.bloodconnect.exception.ConflictException;
import com.bloodconnect.exception.ResourceNotFoundException;
import com.bloodconnect.notification.service.NotificationService;
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

    private static final List<RequestStatus> REPORTABLE_REQUEST_STATUSES =
            List.of(RequestStatus.OPEN, RequestStatus.IN_PROGRESS);

    private static final List<DonationStatus> PENDING_CONFIRMATION_STATUSES =
            List.of(DonationStatus.REPORTED, DonationStatus.PARTIALLY_CONFIRMED);

    private static final List<ResponseStatus> ACTIVE_RESPONSE_STATUSES =
            List.of(ResponseStatus.PENDING, ResponseStatus.ACCEPTED);

    private final DonationRepository donationRepository;
    private final DonorRepository donorRepository;
    private final BloodRequestRepository bloodRequestRepository;
    private final DonationResponseRepository donationResponseRepository;
    private final BloodRequestService bloodRequestService;
    private final DonorService donorService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public DonationHistoryResponse mine(UserPrincipal principal) {
        Donor donor = donorService.findByUserId(principal.getId());
        List<Donation> donations = donationRepository.findByDonorIdOrderByDonationDateDesc(donor.getId());
        List<Donation> counted = donations.stream()
                .filter(donation -> donation.getConfirmedUnits() > 0)
                .toList();
        LocalDate lastDonation = counted.stream()
                .map(Donation::getDonationDate)
                .max(LocalDate::compareTo)
                .orElse(donor.getLastDonationDate());
        return new DonationHistoryResponse(
                counted.size(),
                counted.stream().mapToLong(Donation::getConfirmedUnits).sum(),
                lastDonation,
                lastDonation == null ? null : lastDonation.plusDays(56),
                ORIENTATION_NOTE,
                donations.stream().map(this::toDto).toList()
        );
    }

    @Transactional(readOnly = true)
    public DonationDto get(Long id, UserPrincipal principal) {
        Donation donation = find(id);
        requireViewer(donation, principal);
        return toDto(donation);
    }

    @Transactional(readOnly = true)
    public PageResponse<DonationDto> adminList(Pageable pageable) {
        return PageResponse.from(donationRepository.findAll(pageable).map(this::toDto));
    }

    @Transactional(readOnly = true)
    public List<DonationDto> listForRequest(Long bloodRequestId, UserPrincipal principal) {
        BloodRequest request = bloodRequestService.findEntity(bloodRequestId);
        boolean ownerOrAdmin = isOwnerOrAdmin(request, principal);
        if (ownerOrAdmin) {
            return donationRepository.findByBloodRequestIdOrderByCreatedAtDesc(bloodRequestId).stream()
                    .map(this::toDto)
                    .toList();
        }
        return donorRepository.findByUserId(principal.getId())
                .map(donor -> donationRepository
                        .findByBloodRequestIdAndDonorIdOrderByCreatedAtDesc(bloodRequestId, donor.getId())
                        .stream()
                        .map(this::toDto)
                        .toList())
                .orElse(List.of());
    }

    @Transactional
    public DonationDto report(Long bloodRequestId, UserPrincipal principal, ReportDonationRequest request) {
        BloodRequest bloodRequest = bloodRequestService.findEntity(bloodRequestId);
        Donor donor = donorService.findByUserId(principal.getId());
        if (bloodRequest.getCreatedBy().getId().equals(principal.getId())) {
            throw new BadRequestException("No puede reportar una donación en su propia solicitud");
        }
        return toDto(reportForDonor(
                bloodRequest,
                donor,
                request.units(),
                request.donationDate(),
                request.notes()
        ));
    }

    @Transactional
    public Donation reportForDonor(
            BloodRequest bloodRequest,
            Donor donor,
            int units,
            LocalDate donationDate,
            String notes
    ) {
        requireReportableRequest(bloodRequest);
        if (units <= 0) {
            throw new BadRequestException("Las unidades donadas deben ser mayores que cero");
        }
        long remaining = bloodRequest.getUnitsRequired()
                - donationRepository.sumConfirmedUnitsByRequest(bloodRequest.getId());
        if (remaining <= 0) {
            throw new BadRequestException("Esta solicitud ya no necesita más unidades");
        }
        if (units > remaining) {
            throw new BadRequestException(
                    "Solo puede reportar hasta " + remaining
                            + (remaining == 1 ? " unidad pendiente" : " unidades pendientes")
            );
        }
        LocalDate date = donationDate == null ? LocalDate.now() : donationDate;
        if (date.isAfter(LocalDate.now())) {
            throw new BadRequestException("La fecha de donación no puede estar en el futuro");
        }
        if (donationRepository.existsByBloodRequestIdAndDonorIdAndStatusIn(
                bloodRequest.getId(),
                donor.getId(),
                PENDING_CONFIRMATION_STATUSES
        )) {
            throw new ConflictException(
                    "Ya tiene una donación reportada pendiente de confirmación para esta solicitud"
            );
        }

        Donation donation = donationRepository.save(Donation.builder()
                .donor(donor)
                .bloodRequest(bloodRequest)
                .donationDate(date)
                .units(units)
                .confirmedUnits(0)
                .notes(clean(notes))
                .status(DonationStatus.REPORTED)
                .build());

        completeActiveResponses(bloodRequest, donor);

        if (bloodRequest.getStatus() == RequestStatus.OPEN) {
            bloodRequest.setStatus(RequestStatus.IN_PROGRESS);
            bloodRequestRepository.save(bloodRequest);
        }

        notificationService.create(
                bloodRequest.getCreatedBy(),
                NotificationType.DONATION_REPORTED,
                "Donación reportada",
                donor.getUser().getFirstName() + " " + donor.getUser().getLastName()
                        + " reportó " + units + (units == 1 ? " unidad donada" : " unidades donadas")
                        + ". Confirme las unidades recibidas.",
                "BLOOD_REQUEST",
                bloodRequest.getId()
        );
        return donation;
    }

    @Transactional
    public DonationDto confirm(Long donationId, UserPrincipal principal, ConfirmDonationRequest request) {
        Donation donation = find(donationId);
        BloodRequest bloodRequest = donation.getBloodRequest();
        if (bloodRequest == null) {
            throw new BadRequestException("Esta donación no está asociada a una solicitud");
        }
        bloodRequestService.requireOwnerOrAdmin(bloodRequest, principal);
        if (!REPORTABLE_REQUEST_STATUSES.contains(bloodRequest.getStatus())
                && bloodRequest.getStatus() != RequestStatus.FULFILLED) {
            throw new BadRequestException("Esta solicitud ya no acepta confirmaciones de unidades");
        }
        if (bloodRequest.getStatus() == RequestStatus.FULFILLED) {
            throw new BadRequestException("La solicitud ya está completada");
        }
        if (!PENDING_CONFIRMATION_STATUSES.contains(donation.getStatus())) {
            throw new BadRequestException("Solo se pueden confirmar donaciones reportadas o parciales");
        }

        int confirmedUnits = request.confirmedUnits();
        if (confirmedUnits < donation.getConfirmedUnits()) {
            throw new BadRequestException("Las unidades confirmadas no pueden ser menores que las ya confirmadas");
        }
        if (confirmedUnits > donation.getUnits()) {
            throw new BadRequestException("No puede confirmar más unidades de las que el donante reportó");
        }

        long alreadyCompleted = donationRepository.sumConfirmedUnitsByRequest(bloodRequest.getId())
                - donation.getConfirmedUnits();
        long remainingRequired = Math.max(0, bloodRequest.getUnitsRequired() - alreadyCompleted);
        if (confirmedUnits > remainingRequired) {
            throw new BadRequestException(
                    "Las unidades confirmadas no pueden superar las " + remainingRequired
                            + (remainingRequired == 1 ? " unidad pendiente" : " unidades pendientes")
                            + " de la solicitud"
            );
        }

        donation.setConfirmedUnits(confirmedUnits);
        donation.setStatus(resolveConfirmationStatus(donation.getUnits(), confirmedUnits));
        donationRepository.save(donation);
        updateLastDonationDate(donation);

        DonationProgress progress = applyRequestProgress(bloodRequest);
        notifyConfirmation(donation, bloodRequest, progress);
        return toDto(donation);
    }

    public DonationProgress progressFor(BloodRequest request) {
        return DonationProgress.of(
                request.getUnitsRequired(),
                donationRepository.sumConfirmedUnitsByRequest(request.getId())
        );
    }

    private DonationProgress applyRequestProgress(BloodRequest bloodRequest) {
        DonationProgress progress = progressFor(bloodRequest);
        if (progress.isFulfilled(bloodRequest.getUnitsRequired())) {
            bloodRequest.setStatus(RequestStatus.FULFILLED);
        } else if (bloodRequest.getStatus() == RequestStatus.OPEN && progress.completedUnits() > 0) {
            bloodRequest.setStatus(RequestStatus.IN_PROGRESS);
        }
        bloodRequestRepository.save(bloodRequest);
        return progress;
    }

    private void notifyConfirmation(Donation donation, BloodRequest bloodRequest, DonationProgress progress) {
        boolean fullyConfirmed = donation.getStatus() == DonationStatus.CONFIRMED;
        notificationService.create(
                donation.getDonor().getUser(),
                NotificationType.DONATION_CONFIRMED,
                fullyConfirmed ? "Donación confirmada" : "Donación confirmada parcialmente",
                fullyConfirmed
                        ? "Se confirmaron las " + donation.getConfirmedUnits()
                        + (donation.getConfirmedUnits() == 1 ? " unidad" : " unidades")
                        + " que reportó."
                        : "Se confirmaron " + donation.getConfirmedUnits() + " de "
                        + donation.getUnits() + " unidades reportadas.",
                "DONATION",
                donation.getId()
        );
        if (progress.isFulfilled(bloodRequest.getUnitsRequired())) {
            notificationService.create(
                    bloodRequest.getCreatedBy(),
                    NotificationType.REQUEST_FULFILLED,
                    "Solicitud completada",
                    "Se alcanzaron las unidades requeridas para su solicitud.",
                    "BLOOD_REQUEST",
                    bloodRequest.getId()
            );
        }
    }

    private void updateLastDonationDate(Donation donation) {
        if (donation.getConfirmedUnits() <= 0) {
            return;
        }
        Donor donor = donation.getDonor();
        LocalDate current = donor.getLastDonationDate();
        if (current == null || donation.getDonationDate().isAfter(current)) {
            donor.setLastDonationDate(donation.getDonationDate());
            donorRepository.save(donor);
        }
    }

    private void completeActiveResponses(BloodRequest bloodRequest, Donor donor) {
        List<DonationResponse> responses = donationResponseRepository.findByBloodRequestIdAndDonorIdAndStatusIn(
                bloodRequest.getId(),
                donor.getId(),
                ACTIVE_RESPONSE_STATUSES
        );
        for (DonationResponse response : responses) {
            response.setStatus(ResponseStatus.COMPLETED);
        }
        if (!responses.isEmpty()) {
            donationResponseRepository.saveAll(responses);
        }
    }

    private void requireReportableRequest(BloodRequest bloodRequest) {
        if (!REPORTABLE_REQUEST_STATUSES.contains(bloodRequest.getStatus())) {
            throw new BadRequestException("La solicitud ya no acepta reportes de donación");
        }
        if (bloodRequest.getDeadline().isBefore(java.time.Instant.now())
                && bloodRequest.getStatus() != RequestStatus.IN_PROGRESS) {
            throw new BadRequestException("La solicitud ya no acepta reportes de donación");
        }
    }

    private DonationStatus resolveConfirmationStatus(int reportedUnits, int confirmedUnits) {
        if (confirmedUnits >= reportedUnits) {
            return DonationStatus.CONFIRMED;
        }
        return DonationStatus.PARTIALLY_CONFIRMED;
    }

    private Donation find(Long id) {
        return donationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la donación"));
    }

    private void requireViewer(Donation donation, UserPrincipal principal) {
        if (principal.getRole() == Role.ADMIN) {
            return;
        }
        Long userId = principal.getId();
        if (donation.getDonor().getUser().getId().equals(userId)) {
            return;
        }
        if (donation.getBloodRequest() != null
                && donation.getBloodRequest().getCreatedBy().getId().equals(userId)) {
            return;
        }
        throw new AccessDeniedException("No puede consultar donaciones de otro usuario");
    }

    private boolean isOwnerOrAdmin(BloodRequest request, UserPrincipal principal) {
        return principal.getRole() == Role.ADMIN
                || request.getCreatedBy().getId().equals(principal.getId());
    }

    private DonationDto toDto(Donation donation) {
        var request = donation.getBloodRequest();
        return new DonationDto(
                donation.getId(),
                donation.getDonor().getId(),
                donation.getDonor().getUser().getId(),
                donation.getDonor().getUser().getFirstName() + " "
                        + donation.getDonor().getUser().getLastName(),
                request == null ? null : request.getId(),
                request == null ? null : request.getPatientName(),
                request == null
                        ? (donation.getDonationCenter() == null ? null : donation.getDonationCenter().getName())
                        : request.getHospital(),
                request == null
                        ? null
                        : request.getCreatedBy().getFirstName() + " " + request.getCreatedBy().getLastName(),
                donation.getDonationCenter() == null ? null : donation.getDonationCenter().getId(),
                donation.getDonationCenter() == null ? null : donation.getDonationCenter().getName(),
                donation.getDonationDate(),
                donation.getUnits(),
                donation.getConfirmedUnits(),
                donation.getNotes(),
                donation.getStatus(),
                donation.getCreatedAt(),
                donation.getUpdatedAt()
        );
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

package com.bloodconnect.donationresponse.service;

import com.bloodconnect.bloodrequest.entity.BloodRequest;
import com.bloodconnect.bloodrequest.repository.BloodRequestRepository;
import com.bloodconnect.bloodrequest.service.BloodRequestService;
import com.bloodconnect.common.enums.NotificationType;
import com.bloodconnect.common.enums.RequestStatus;
import com.bloodconnect.common.enums.ResponseStatus;
import com.bloodconnect.common.enums.Role;
import com.bloodconnect.donation.service.DonationService;
import com.bloodconnect.donationresponse.dto.CreateDonationResponseRequest;
import com.bloodconnect.donationresponse.dto.DonationResponseDto;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DonationResponseService {

    private static final List<ResponseStatus> ACTIVE_STATUSES =
            List.of(ResponseStatus.PENDING, ResponseStatus.ACCEPTED);

    private final DonationResponseRepository responseRepository;
    private final BloodRequestRepository bloodRequestRepository;
    private final BloodRequestService bloodRequestService;
    private final DonorService donorService;
    private final DonorRepository donorRepository;
    private final DonationService donationService;
    private final NotificationService notificationService;

    @Transactional
    public DonationResponseDto create(
            Long bloodRequestId,
            UserPrincipal principal,
            CreateDonationResponseRequest request
    ) {
        BloodRequest bloodRequest = bloodRequestService.findEntity(bloodRequestId);
        Donor donor = donorService.findByUserId(principal.getId());
        if (bloodRequest.getCreatedBy().getId().equals(principal.getId())) {
            throw new BadRequestException("No puede responder a su propia solicitud");
        }
        if (!List.of(RequestStatus.OPEN, RequestStatus.IN_PROGRESS).contains(bloodRequest.getStatus())
                || bloodRequest.getDeadline().isBefore(Instant.now())) {
            throw new BadRequestException("La solicitud ya no acepta respuestas");
        }
        if (responseRepository.existsByBloodRequestIdAndDonorIdAndStatusIn(
                bloodRequestId,
                donor.getId(),
                ACTIVE_STATUSES
        )) {
            throw new ConflictException("Ya ofreciste ayudar con esta solicitud");
        }
        DonationResponse response = DonationResponse.builder()
                .bloodRequest(bloodRequest)
                .donor(donor)
                .status(ResponseStatus.PENDING)
                .message(clean(request.message()))
                .build();
        try {
            response = responseRepository.saveAndFlush(response);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Ya ofreciste ayudar con esta solicitud");
        }
        String donorName = donor.getUser().getFirstName() + " " + donor.getUser().getLastName();
        notificationService.create(
                bloodRequest.getCreatedBy(),
                NotificationType.RESPONSE_RECEIVED,
                donorName + " quiere ayudar",
                donorName + " quiere ayudar con tu solicitud de sangre.",
                "BLOOD_REQUEST",
                bloodRequest.getId()
        );
        return toDto(response);
    }

    @Transactional(readOnly = true)
    public List<DonationResponseDto> listForRequest(Long requestId, UserPrincipal principal) {
        BloodRequest request = bloodRequestService.findEntity(requestId);
        boolean ownerOrAdmin = principal.getRole() == Role.ADMIN
                || request.getCreatedBy().getId().equals(principal.getId());
        if (ownerOrAdmin) {
            return responseRepository.findByBloodRequestIdOrderByCreatedAtDesc(requestId).stream()
                    .map(this::toDto)
                    .toList();
        }
        return donorRepository.findByUserId(principal.getId())
                .map(donor -> responseRepository
                        .findByBloodRequestIdAndDonorIdOrderByCreatedAtDesc(requestId, donor.getId())
                        .stream()
                        .map(this::toDto)
                        .toList())
                .orElse(List.of());
    }

    @Transactional
    public DonationResponseDto accept(Long id, UserPrincipal principal) {
        DonationResponse response = find(id);
        bloodRequestService.requireOwnerOrAdmin(response.getBloodRequest(), principal);
        requireStatus(response, ResponseStatus.PENDING, "Solo se pueden aceptar respuestas pendientes");
        response.setStatus(ResponseStatus.ACCEPTED);
        if (response.getBloodRequest().getStatus() == RequestStatus.OPEN) {
            response.getBloodRequest().setStatus(RequestStatus.IN_PROGRESS);
            bloodRequestRepository.save(response.getBloodRequest());
        }
        notificationService.create(
                response.getDonor().getUser(),
                NotificationType.RESPONSE_ACCEPTED,
                "Respuesta aceptada",
                "Su respuesta de donación fue aceptada. Coordine los próximos pasos con el solicitante.",
                "DONATION_RESPONSE",
                response.getId()
        );
        return toDto(responseRepository.save(response));
    }

    @Transactional
    public DonationResponseDto reject(Long id, UserPrincipal principal) {
        DonationResponse response = find(id);
        bloodRequestService.requireOwnerOrAdmin(response.getBloodRequest(), principal);
        requireStatus(response, ResponseStatus.PENDING, "Solo se pueden rechazar respuestas pendientes");
        response.setStatus(ResponseStatus.REJECTED);
        notificationService.create(
                response.getDonor().getUser(),
                NotificationType.RESPONSE_REJECTED,
                "Respuesta no seleccionada",
                "Su respuesta de donación no fue seleccionada para esta solicitud.",
                "DONATION_RESPONSE",
                response.getId()
        );
        return toDto(responseRepository.save(response));
    }

    @Transactional
    public DonationResponseDto complete(Long id, UserPrincipal principal, int units) {
        DonationResponse response = find(id);
        requireDonorRequestOwnerOrAdmin(response, principal);
        requireStatus(response, ResponseStatus.ACCEPTED, "Solo se pueden completar respuestas aceptadas");
        donationService.reportForDonor(
                response.getBloodRequest(),
                response.getDonor(),
                units,
                LocalDate.now(),
                "Donación reportada al completar la respuesta"
        );
        notificationService.create(
                response.getDonor().getUser(),
                NotificationType.RESPONSE_COMPLETED,
                "Donación reportada",
                "Su donación fue reportada. El solicitante confirmará las unidades recibidas.",
                "BLOOD_REQUEST",
                response.getBloodRequest().getId()
        );
        return toDto(find(id));
    }

    @Transactional
    public DonationResponseDto cancel(Long id, UserPrincipal principal) {
        DonationResponse response = find(id);
        if (principal.getRole() != Role.ADMIN
                && !response.getDonor().getUser().getId().equals(principal.getId())) {
            throw new AccessDeniedException("No puede cancelar respuestas de otro donante");
        }
        if (!ACTIVE_STATUSES.contains(response.getStatus())) {
            throw new BadRequestException("Solo se pueden cancelar respuestas pendientes o aceptadas");
        }
        response.setStatus(ResponseStatus.CANCELLED);
        notificationService.create(
                response.getBloodRequest().getCreatedBy(),
                NotificationType.RESPONSE_CANCELLED,
                "Respuesta cancelada",
                "Un donante canceló su respuesta a la solicitud.",
                "BLOOD_REQUEST",
                response.getBloodRequest().getId()
        );
        return toDto(responseRepository.save(response));
    }

    private DonationResponse find(Long id) {
        return responseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la respuesta de donación"
                ));
    }

    private void requireDonorRequestOwnerOrAdmin(DonationResponse response, UserPrincipal principal) {
        boolean donorOwner = response.getDonor().getUser().getId().equals(principal.getId());
        boolean requestOwner = response.getBloodRequest().getCreatedBy().getId().equals(principal.getId());
        if (principal.getRole() != Role.ADMIN && !donorOwner && !requestOwner) {
            throw new AccessDeniedException("No puede completar respuestas de otros usuarios");
        }
    }

    private void requireStatus(DonationResponse response, ResponseStatus expected, String message) {
        if (response.getStatus() != expected) {
            throw new BadRequestException(message);
        }
    }

    private DonationResponseDto toDto(DonationResponse response) {
        Donor donor = response.getDonor();
        return new DonationResponseDto(
                response.getId(),
                response.getBloodRequest().getId(),
                donor.getId(),
                donor.getUser().getId(),
                donor.getUser().getFirstName() + " " + donor.getUser().getLastName(),
                donor.getPhone(),
                donor.getBloodType(),
                response.getStatus(),
                response.getMessage(),
                response.getCreatedAt(),
                response.getUpdatedAt()
        );
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

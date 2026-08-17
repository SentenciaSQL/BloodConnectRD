package com.bloodconnect.bloodrequest.service;

import com.bloodconnect.bloodrequest.dto.BloodRequestResponse;
import com.bloodconnect.bloodrequest.dto.CreateBloodRequest;
import com.bloodconnect.bloodrequest.dto.DonationProgress;
import com.bloodconnect.bloodrequest.dto.UpdateBloodRequest;
import com.bloodconnect.bloodrequest.entity.BloodRequest;
import com.bloodconnect.bloodrequest.repository.BloodRequestRepository;
import com.bloodconnect.common.dto.PageResponse;
import com.bloodconnect.common.enums.BloodType;
import com.bloodconnect.common.enums.RequestStatus;
import com.bloodconnect.common.enums.Role;
import com.bloodconnect.common.enums.Urgency;
import com.bloodconnect.common.util.GeoUtils;
import com.bloodconnect.common.util.PhoneNormalizer;
import com.bloodconnect.donation.repository.DonationRepository;
import com.bloodconnect.donor.entity.Donor;
import com.bloodconnect.donor.service.BloodCompatibilityService;
import com.bloodconnect.donor.service.DonorService;
import com.bloodconnect.exception.BadRequestException;
import com.bloodconnect.exception.ResourceNotFoundException;
import com.bloodconnect.location.entity.Municipality;
import com.bloodconnect.location.entity.Province;
import com.bloodconnect.location.repository.MunicipalityRepository;
import com.bloodconnect.location.repository.ProvinceRepository;
import com.bloodconnect.security.UserPrincipal;
import com.bloodconnect.user.entity.User;
import com.bloodconnect.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class BloodRequestService {

    private static final List<RequestStatus> ACTIVE_STATUSES =
            List.of(RequestStatus.OPEN, RequestStatus.IN_PROGRESS);

    private final BloodRequestRepository bloodRequestRepository;
    private final DonationRepository donationRepository;
    private final UserRepository userRepository;
    private final ProvinceRepository provinceRepository;
    private final MunicipalityRepository municipalityRepository;
    private final DonorService donorService;
    private final BloodCompatibilityService compatibilityService;
    private final BloodRequestAlertService bloodRequestAlertService;

    @Transactional
    public BloodRequestResponse create(UserPrincipal principal, CreateBloodRequest request) {
        validateCoordinates(request.latitude(), request.longitude());
        Province province = findProvince(request.provinceId());
        Municipality municipality = findMunicipality(request.municipalityId(), province.getId());
        User user = findUser(principal.getId());
        BloodRequest entity = BloodRequest.builder()
                .createdBy(user)
                .patientName(request.patientName().trim())
                .bloodType(request.bloodType())
                .unitsRequired(request.unitsRequired())
                .hospital(request.hospital().trim())
                .province(province)
                .municipality(municipality)
                .sector(clean(request.sector()))
                .address(request.address().trim())
                .reference(clean(request.reference()))
                .latitude(request.latitude())
                .longitude(request.longitude())
                .deadline(request.deadline())
                .description(clean(request.description()))
                .contactPhone(normalizePhone(request.contactPhone()))
                .urgency(request.urgency())
                .status(RequestStatus.OPEN)
                .build();
        BloodRequest saved = bloodRequestRepository.save(entity);
        bloodRequestAlertService.notifyCompatibleDonors(saved);
        return toResponse(saved, null, null);
    }

    @Transactional(readOnly = true)
    public PageResponse<BloodRequestResponse> list(
            BloodType bloodType,
            Long provinceId,
            Long municipalityId,
            Urgency urgency,
            List<RequestStatus> statuses,
            String search,
            Pageable pageable
    ) {
        Page<BloodRequestResponse> page = bloodRequestRepository.findAll(
                buildSpecification(
                        bloodType == null ? List.of() : List.of(bloodType),
                        provinceId,
                        municipalityId,
                        urgency == null ? List.of() : List.of(urgency),
                        statuses == null ? List.of() : statuses,
                        null,
                        search,
                        false
                ),
                pageable
        ).map(request -> toResponse(request, null, null));
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public BloodRequestResponse get(Long id) {
        return toResponse(findEntity(id), null, null);
    }

    @Transactional(readOnly = true)
    public PageResponse<BloodRequestResponse> urgent(Pageable pageable) {
        Page<BloodRequestResponse> page = bloodRequestRepository.findAll(
                buildSpecification(
                        List.of(),
                        null,
                        null,
                        List.of(Urgency.HIGH, Urgency.CRITICAL),
                        ACTIVE_STATUSES,
                        null,
                        null,
                        true
                ),
                pageable
        ).map(request -> toResponse(request, null, null));
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public PageResponse<BloodRequestResponse> compatible(UserPrincipal principal, Pageable pageable) {
        Donor donor = donorService.findByUserId(principal.getId());
        List<BloodType> recipientTypes = compatibilityService.canDonateTo(donor.getBloodType());
        Page<BloodRequestResponse> page = bloodRequestRepository.findAll(
                buildSpecification(
                        recipientTypes,
                        null,
                        null,
                        List.of(),
                        ACTIVE_STATUSES,
                        null,
                        null,
                        true
                ),
                pageable
        ).map(request -> toResponse(
                request,
                donor.getLatitude() == null ? null : donor.getLatitude().doubleValue(),
                donor.getLongitude() == null ? null : donor.getLongitude().doubleValue()
        ));
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public List<BloodRequestResponse> nearby(double latitude, double longitude, double radiusKm) {
        validateGeoSearch(latitude, longitude, radiusKm);
        return bloodRequestRepository.findAll(
                        buildSpecification(
                                List.of(),
                                null,
                                null,
                                List.of(),
                                ACTIVE_STATUSES,
                                null,
                                null,
                                true
                        ),
                        Sort.by(Sort.Direction.DESC, "urgency", "createdAt")
                ).stream()
                .filter(request -> request.getLatitude() != null && request.getLongitude() != null)
                .map(request -> toResponse(request, latitude, longitude))
                .filter(response -> response.approximateDistanceKm() <= radiusKm)
                .sorted(Comparator.comparing(BloodRequestResponse::approximateDistanceKm))
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<BloodRequestResponse> mine(UserPrincipal principal, Pageable pageable) {
        Page<BloodRequestResponse> page = bloodRequestRepository.findAll(
                buildSpecification(
                        List.of(),
                        null,
                        null,
                        List.of(),
                        List.of(),
                        principal.getId(),
                        null,
                        false
                ),
                pageable
        ).map(request -> toResponse(request, null, null));
        return PageResponse.from(page);
    }

    @Transactional
    public BloodRequestResponse update(Long id, UserPrincipal principal, UpdateBloodRequest request) {
        BloodRequest entity = findEntity(id);
        requireOwnerOrAdmin(entity, principal);
        if (!ACTIVE_STATUSES.contains(entity.getStatus())) {
            throw new BadRequestException("Solo se pueden editar solicitudes abiertas o en progreso");
        }
        validateCoordinates(request.latitude(), request.longitude());
        Province province = findProvince(request.provinceId());
        Municipality municipality = findMunicipality(request.municipalityId(), province.getId());
        entity.setPatientName(request.patientName().trim());
        entity.setBloodType(request.bloodType());
        long confirmedUnits = donationRepository.sumConfirmedUnitsByRequest(entity.getId());
        if (request.unitsRequired() < confirmedUnits) {
            throw new BadRequestException(
                    "Las unidades requeridas no pueden ser menores que las ya confirmadas"
            );
        }
        entity.setUnitsRequired(request.unitsRequired());
        if (confirmedUnits >= request.unitsRequired()) {
            entity.setStatus(RequestStatus.FULFILLED);
        }
        entity.setHospital(request.hospital().trim());
        entity.setProvince(province);
        entity.setMunicipality(municipality);
        entity.setSector(clean(request.sector()));
        entity.setAddress(request.address().trim());
        entity.setReference(clean(request.reference()));
        entity.setLatitude(request.latitude());
        entity.setLongitude(request.longitude());
        entity.setDeadline(request.deadline());
        entity.setDescription(clean(request.description()));
        entity.setContactPhone(normalizePhone(request.contactPhone()));
        entity.setUrgency(request.urgency());
        return toResponse(bloodRequestRepository.save(entity), null, null);
    }

    @Transactional
    public BloodRequestResponse cancel(Long id, UserPrincipal principal) {
        BloodRequest entity = findEntity(id);
        requireOwnerOrAdmin(entity, principal);
        if (entity.getStatus() == RequestStatus.FULFILLED) {
            throw new BadRequestException("Una solicitud completada no puede ser cancelada");
        }
        entity.setStatus(RequestStatus.CANCELLED);
        return toResponse(bloodRequestRepository.save(entity), null, null);
    }

    @Transactional(readOnly = true)
    public BloodRequest findEntity(Long id) {
        return bloodRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la solicitud de sangre"
                ));
    }

    public void requireOwnerOrAdmin(BloodRequest request, UserPrincipal principal) {
        if (principal.getRole() != Role.ADMIN
                && !request.getCreatedBy().getId().equals(principal.getId())) {
            throw new AccessDeniedException("No puede modificar solicitudes de otro usuario");
        }
    }

    private Specification<BloodRequest> buildSpecification(
            List<BloodType> bloodTypes,
            Long provinceId,
            Long municipalityId,
            List<Urgency> urgencies,
            List<RequestStatus> statuses,
            Long createdById,
            String search,
            boolean futureOnly
    ) {
        Specification<BloodRequest> specification = (root, query, cb) -> cb.conjunction();
        if (!bloodTypes.isEmpty()) {
            specification = specification.and((root, query, cb) -> root.get("bloodType").in(bloodTypes));
        }
        if (provinceId != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("province").get("id"), provinceId));
        }
        if (municipalityId != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("municipality").get("id"), municipalityId));
        }
        if (!urgencies.isEmpty()) {
            specification = specification.and((root, query, cb) -> root.get("urgency").in(urgencies));
        }
        if (!statuses.isEmpty()) {
            specification = specification.and((root, query, cb) -> root.get("status").in(statuses));
        }
        if (createdById != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("createdBy").get("id"), createdById));
        }
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("patientName")), pattern),
                    cb.like(cb.lower(root.get("hospital")), pattern),
                    cb.like(cb.lower(root.get("sector")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            ));
        }
        if (futureOnly) {
            specification = specification.and((root, query, cb) -> cb.greaterThan(root.get("deadline"), Instant.now()));
        }
        return specification;
    }

    private BloodRequestResponse toResponse(BloodRequest request, Double latitude, Double longitude) {
        Double distance = null;
        if (latitude != null && request.getLatitude() != null && request.getLongitude() != null) {
            distance = Math.round(GeoUtils.haversineKm(
                    latitude,
                    longitude,
                    request.getLatitude().doubleValue(),
                    request.getLongitude().doubleValue()
            ) * 100.0) / 100.0;
        }
        User creator = request.getCreatedBy();
        DonationProgress progress = DonationProgress.of(
                request.getUnitsRequired(),
                donationRepository.sumConfirmedUnitsByRequest(request.getId())
        );
        return new BloodRequestResponse(
                request.getId(),
                creator.getId(),
                creator.getFirstName() + " " + creator.getLastName(),
                request.getPatientName(),
                request.getBloodType(),
                request.getUnitsRequired(),
                progress.completedUnits(),
                progress.pendingUnits(),
                progress.progressPercent(),
                progress.progress(),
                request.getHospital(),
                request.getProvince().getId(),
                request.getProvince().getName(),
                request.getMunicipality().getId(),
                request.getMunicipality().getName(),
                request.getSector(),
                request.getAddress(),
                request.getReference(),
                request.getLatitude(),
                request.getLongitude(),
                request.getDeadline(),
                request.getDescription(),
                request.getContactPhone(),
                request.getUrgency(),
                request.getStatus(),
                distance,
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }

    private Province findProvince(Long id) {
        return provinceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la provincia solicitada"));
    }

    private Municipality findMunicipality(Long id, Long provinceId) {
        Municipality municipality = municipalityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el municipio solicitado"));
        if (!municipality.getProvince().getId().equals(provinceId)) {
            throw new BadRequestException("El municipio no pertenece a la provincia seleccionada");
        }
        return municipality;
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el usuario autenticado"));
    }

    private String normalizePhone(String phone) {
        try {
            return PhoneNormalizer.normalize(phone);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException(exception.getMessage());
        }
    }

    private void validateCoordinates(BigDecimal latitude, BigDecimal longitude) {
        if ((latitude == null) != (longitude == null)) {
            throw new BadRequestException("Debe indicar latitud y longitud juntas");
        }
    }

    private void validateGeoSearch(double latitude, double longitude, double radiusKm) {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new BadRequestException("Las coordenadas indicadas no son válidas");
        }
        if (radiusKm <= 0 || radiusKm > 500) {
            throw new BadRequestException("El radio debe ser mayor que cero y no exceder 500 km");
        }
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

package com.bloodconnect.donor.service;

import com.bloodconnect.common.dto.PageResponse;
import com.bloodconnect.common.enums.AvailabilityStatus;
import com.bloodconnect.common.enums.BloodType;
import com.bloodconnect.common.enums.Role;
import com.bloodconnect.common.util.GeoUtils;
import com.bloodconnect.common.util.PhoneNormalizer;
import com.bloodconnect.donor.dto.CreateDonorRequest;
import com.bloodconnect.donor.dto.DonorPrivateResponse;
import com.bloodconnect.donor.dto.DonorResponse;
import com.bloodconnect.donor.dto.UpdateAvailabilityRequest;
import com.bloodconnect.donor.dto.UpdateDonorRequest;
import com.bloodconnect.donor.entity.Donor;
import com.bloodconnect.donor.repository.DonorRepository;
import com.bloodconnect.exception.BadRequestException;
import com.bloodconnect.exception.ConflictException;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DonorService {

    private final DonorRepository donorRepository;
    private final UserRepository userRepository;
    private final ProvinceRepository provinceRepository;
    private final MunicipalityRepository municipalityRepository;
    private final BloodCompatibilityService compatibilityService;

    @Transactional
    public DonorPrivateResponse create(UserPrincipal principal, CreateDonorRequest request) {
        if (donorRepository.existsByUserId(principal.getId())) {
            throw new ConflictException("El usuario ya tiene un perfil de donante");
        }
        validateDonationDate(request.lastDonationDate());
        validateCoordinates(request.latitude(), request.longitude());
        Province province = findProvince(request.provinceId());
        Municipality municipality = findMunicipality(request.municipalityId(), province.getId());
        User user = findUser(principal.getId());

        Donor donor = Donor.builder()
                .user(user)
                .bloodType(request.bloodType())
                .birthDate(request.birthDate())
                .sex(request.sex())
                .phone(normalizePhone(request.phone()))
                .province(province)
                .municipality(municipality)
                .sector(clean(request.sector()))
                .approximateAddress(clean(request.approximateAddress()))
                .latitude(request.latitude())
                .longitude(request.longitude())
                .lastDonationDate(request.lastDonationDate())
                .availability(AvailabilityStatus.AVAILABLE)
                .build();
        if (user.getRole() == Role.USER) {
            user.setRole(Role.DONOR);
            userRepository.save(user);
        }
        return toPrivate(donorRepository.save(donor));
    }

    @Transactional(readOnly = true)
    public PageResponse<DonorResponse> list(
            BloodType bloodType,
            Long provinceId,
            Long municipalityId,
            AvailabilityStatus availability,
            Double latitude,
            Double longitude,
            Pageable pageable
    ) {
        validateCoordinatePair(latitude, longitude);
        Page<DonorResponse> page = donorRepository.findAll(
                buildSpecification(List.of(), bloodType, provinceId, municipalityId, availability),
                pageable
        ).map(donor -> toPublic(donor, latitude, longitude));
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public DonorResponse getPublic(Long id) {
        return toPublic(findById(id), null, null);
    }

    @Transactional(readOnly = true)
    public DonorPrivateResponse getMine(UserPrincipal principal) {
        return toPrivate(findByUserId(principal.getId()));
    }

    @Transactional
    public DonorPrivateResponse updateMine(UserPrincipal principal, UpdateDonorRequest request) {
        validateDonationDate(request.lastDonationDate());
        validateCoordinates(request.latitude(), request.longitude());
        Donor donor = findByUserId(principal.getId());
        Province province = findProvince(request.provinceId());
        Municipality municipality = findMunicipality(request.municipalityId(), province.getId());
        donor.setBloodType(request.bloodType());
        donor.setBirthDate(request.birthDate());
        donor.setSex(request.sex());
        donor.setPhone(normalizePhone(request.phone()));
        donor.setProvince(province);
        donor.setMunicipality(municipality);
        donor.setSector(clean(request.sector()));
        donor.setApproximateAddress(clean(request.approximateAddress()));
        donor.setLatitude(request.latitude());
        donor.setLongitude(request.longitude());
        donor.setLastDonationDate(request.lastDonationDate());
        return toPrivate(donorRepository.save(donor));
    }

    @Transactional
    public DonorPrivateResponse updateAvailability(
            UserPrincipal principal,
            UpdateAvailabilityRequest request
    ) {
        Donor donor = findByUserId(principal.getId());
        donor.setAvailability(request.availability());
        return toPrivate(donorRepository.save(donor));
    }

    @Transactional(readOnly = true)
    public List<DonorResponse> compatible(
            BloodType recipientBloodType,
            Long provinceId,
            Long municipalityId,
            AvailabilityStatus availability,
            Double latitude,
            Double longitude,
            Double radiusKm
    ) {
        validateCoordinatePair(latitude, longitude);
        if (radiusKm != null && (latitude == null || radiusKm <= 0)) {
            throw new BadRequestException(
                    "Para filtrar por radio debe indicar coordenadas y un radio mayor que cero"
            );
        }
        List<BloodType> donorTypes = compatibilityService.canReceiveFrom(recipientBloodType);
        return donorRepository.findAll(
                        buildSpecification(donorTypes, null, provinceId, municipalityId, availability),
                        Sort.by(Sort.Direction.DESC, "createdAt")
                ).stream()
                .map(donor -> toPublic(donor, latitude, longitude))
                .filter(response -> radiusKm == null
                        || response.approximateDistanceKm() != null
                        && response.approximateDistanceKm() <= radiusKm)
                .sorted(Comparator.comparing(
                        DonorResponse::approximateDistanceKm,
                        Comparator.nullsLast(Double::compareTo)
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<Donor> findOptionalByUserId(Long userId) {
        return donorRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Donor findByUserId(Long userId) {
        return findOptionalByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El usuario autenticado no tiene un perfil de donante"
                ));
    }

    @Transactional(readOnly = true)
    public Donor findById(Long id) {
        return donorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el donante solicitado"));
    }

    private Specification<Donor> buildSpecification(
            List<BloodType> bloodTypes,
            BloodType bloodType,
            Long provinceId,
            Long municipalityId,
            AvailabilityStatus availability
    ) {
        Specification<Donor> specification = (root, query, cb) -> cb.conjunction();
        if (!bloodTypes.isEmpty()) {
            specification = specification.and((root, query, cb) -> root.get("bloodType").in(bloodTypes));
        }
        if (bloodType != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("bloodType"), bloodType));
        }
        if (provinceId != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("province").get("id"), provinceId));
        }
        if (municipalityId != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("municipality").get("id"), municipalityId));
        }
        if (availability != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("availability"), availability));
        }
        return specification;
    }

    private DonorResponse toPublic(Donor donor, Double latitude, Double longitude) {
        Double distance = null;
        if (latitude != null && donor.getLatitude() != null && donor.getLongitude() != null) {
            distance = roundDistance(GeoUtils.haversineKm(
                    latitude,
                    longitude,
                    donor.getLatitude().doubleValue(),
                    donor.getLongitude().doubleValue()
            ));
        }
        return new DonorResponse(
                donor.getId(),
                donor.getBloodType(),
                donor.getProvince().getId(),
                donor.getProvince().getName(),
                donor.getMunicipality().getId(),
                donor.getMunicipality().getName(),
                donor.getAvailability(),
                donor.getLastDonationDate(),
                distance
        );
    }

    private DonorPrivateResponse toPrivate(Donor donor) {
        User user = donor.getUser();
        return new DonorPrivateResponse(
                donor.getId(),
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                donor.getBloodType(),
                donor.getBirthDate(),
                donor.getSex(),
                donor.getPhone(),
                donor.getProvince().getId(),
                donor.getProvince().getName(),
                donor.getMunicipality().getId(),
                donor.getMunicipality().getName(),
                donor.getSector(),
                donor.getApproximateAddress(),
                donor.getLatitude(),
                donor.getLongitude(),
                donor.getLastDonationDate(),
                donor.getAvailability(),
                donor.getCreatedAt(),
                donor.getUpdatedAt()
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

    private void validateCoordinatePair(Double latitude, Double longitude) {
        if ((latitude == null) != (longitude == null)) {
            throw new BadRequestException("Debe indicar latitud y longitud juntas");
        }
        if (latitude != null && (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180)) {
            throw new BadRequestException("Las coordenadas indicadas no son válidas");
        }
    }

    private void validateDonationDate(LocalDate date) {
        if (date != null && date.isAfter(LocalDate.now())) {
            throw new BadRequestException("La fecha de última donación no puede estar en el futuro");
        }
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private double roundDistance(double distance) {
        return Math.round(distance * 100.0) / 100.0;
    }
}

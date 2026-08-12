package com.bloodconnect.donationcenter.service;

import com.bloodconnect.auth.dto.MessageResponse;
import com.bloodconnect.common.dto.PageResponse;
import com.bloodconnect.common.enums.CenterType;
import com.bloodconnect.common.util.GeoUtils;
import com.bloodconnect.common.util.PhoneNormalizer;
import com.bloodconnect.donationcenter.dto.DonationCenterRequest;
import com.bloodconnect.donationcenter.dto.DonationCenterResponse;
import com.bloodconnect.donationcenter.entity.DonationCenter;
import com.bloodconnect.donationcenter.repository.DonationCenterRepository;
import com.bloodconnect.exception.BadRequestException;
import com.bloodconnect.exception.ResourceNotFoundException;
import com.bloodconnect.location.entity.Municipality;
import com.bloodconnect.location.entity.Province;
import com.bloodconnect.location.repository.MunicipalityRepository;
import com.bloodconnect.location.repository.ProvinceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DonationCenterService {

    private final DonationCenterRepository centerRepository;
    private final ProvinceRepository provinceRepository;
    private final MunicipalityRepository municipalityRepository;

    @Transactional(readOnly = true)
    public PageResponse<DonationCenterResponse> list(
            Long provinceId,
            Long municipalityId,
            CenterType type,
            Pageable pageable
    ) {
        return PageResponse.from(centerRepository.findAll(
                specification(provinceId, municipalityId, type, true),
                pageable
        ).map(center -> toResponse(center, null, null)));
    }

    @Transactional(readOnly = true)
    public DonationCenterResponse getPublic(Long id) {
        DonationCenter center = find(id);
        if (!center.isActive()) {
            throw new ResourceNotFoundException("No se encontró el centro de donación");
        }
        return toResponse(center, null, null);
    }

    @Transactional(readOnly = true)
    public List<DonationCenterResponse> nearby(double latitude, double longitude, double radiusKm) {
        validateGeoSearch(latitude, longitude, radiusKm);
        return centerRepository.findAll(
                        specification(null, null, null, true),
                        Sort.by("name")
                ).stream()
                .filter(center -> center.getLatitude() != null && center.getLongitude() != null)
                .map(center -> toResponse(center, latitude, longitude))
                .filter(center -> center.approximateDistanceKm() <= radiusKm)
                .sorted(Comparator.comparing(DonationCenterResponse::approximateDistanceKm))
                .toList();
    }

    @Transactional
    public DonationCenterResponse create(DonationCenterRequest request) {
        validateCoordinates(request.latitude(), request.longitude());
        Province province = findProvince(request.provinceId());
        Municipality municipality = findMunicipality(request.municipalityId(), province.getId());
        DonationCenter center = DonationCenter.builder()
                .name(request.name().trim())
                .type(request.type())
                .province(province)
                .municipality(municipality)
                .sector(clean(request.sector()))
                .address(request.address().trim())
                .reference(clean(request.reference()))
                .phone(normalizeOptionalPhone(request.phone()))
                .schedule(clean(request.schedule()))
                .latitude(request.latitude())
                .longitude(request.longitude())
                .active(request.active() == null || request.active())
                .build();
        return toResponse(centerRepository.save(center), null, null);
    }

    @Transactional
    public DonationCenterResponse update(Long id, DonationCenterRequest request) {
        validateCoordinates(request.latitude(), request.longitude());
        DonationCenter center = find(id);
        Province province = findProvince(request.provinceId());
        Municipality municipality = findMunicipality(request.municipalityId(), province.getId());
        center.setName(request.name().trim());
        center.setType(request.type());
        center.setProvince(province);
        center.setMunicipality(municipality);
        center.setSector(clean(request.sector()));
        center.setAddress(request.address().trim());
        center.setReference(clean(request.reference()));
        center.setPhone(normalizeOptionalPhone(request.phone()));
        center.setSchedule(clean(request.schedule()));
        center.setLatitude(request.latitude());
        center.setLongitude(request.longitude());
        if (request.active() != null) {
            center.setActive(request.active());
        }
        return toResponse(centerRepository.save(center), null, null);
    }

    @Transactional
    public MessageResponse delete(Long id) {
        DonationCenter center = find(id);
        center.setActive(false);
        centerRepository.save(center);
        return new MessageResponse("Centro de donación desactivado correctamente");
    }

    private Specification<DonationCenter> specification(
            Long provinceId,
            Long municipalityId,
            CenterType type,
            Boolean active
    ) {
        Specification<DonationCenter> specification = (root, query, cb) -> cb.conjunction();
        if (provinceId != null) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("province").get("id"), provinceId));
        }
        if (municipalityId != null) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("municipality").get("id"), municipalityId));
        }
        if (type != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("type"), type));
        }
        if (active != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("active"), active));
        }
        return specification;
    }

    private DonationCenter find(Long id) {
        return centerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el centro de donación"
                ));
    }

    private DonationCenterResponse toResponse(
            DonationCenter center,
            Double latitude,
            Double longitude
    ) {
        Double distance = null;
        if (latitude != null && center.getLatitude() != null && center.getLongitude() != null) {
            distance = Math.round(GeoUtils.haversineKm(
                    latitude,
                    longitude,
                    center.getLatitude().doubleValue(),
                    center.getLongitude().doubleValue()
            ) * 100.0) / 100.0;
        }
        return new DonationCenterResponse(
                center.getId(),
                center.getName(),
                center.getType(),
                center.getProvince().getId(),
                center.getProvince().getName(),
                center.getMunicipality().getId(),
                center.getMunicipality().getName(),
                center.getSector(),
                center.getAddress(),
                center.getReference(),
                center.getPhone(),
                center.getSchedule(),
                center.getLatitude(),
                center.getLongitude(),
                center.isActive(),
                distance,
                center.getCreatedAt(),
                center.getUpdatedAt()
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

    private String normalizeOptionalPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
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

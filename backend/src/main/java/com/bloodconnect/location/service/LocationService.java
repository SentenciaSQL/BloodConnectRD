package com.bloodconnect.location.service;

import com.bloodconnect.exception.ResourceNotFoundException;
import com.bloodconnect.location.dto.MunicipalityResponse;
import com.bloodconnect.location.dto.ProvinceResponse;
import com.bloodconnect.location.entity.Municipality;
import com.bloodconnect.location.entity.Province;
import com.bloodconnect.location.repository.MunicipalityRepository;
import com.bloodconnect.location.repository.ProvinceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationService {

    private final ProvinceRepository provinceRepository;
    private final MunicipalityRepository municipalityRepository;

    public List<ProvinceResponse> getProvinces() {
        return provinceRepository.findAllByOrderByNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<MunicipalityResponse> getMunicipalities(Long provinceId) {
        if (!provinceRepository.existsById(provinceId)) {
            throw new ResourceNotFoundException("No se encontró la provincia solicitada");
        }

        return municipalityRepository.findByProvinceIdOrderByNameAsc(provinceId).stream()
                .map(this::toResponse)
                .toList();
    }

    private ProvinceResponse toResponse(Province province) {
        return new ProvinceResponse(province.getId(), province.getCode(), province.getName());
    }

    private MunicipalityResponse toResponse(Municipality municipality) {
        return new MunicipalityResponse(
                municipality.getId(),
                municipality.getProvince().getId(),
                municipality.getCode(),
                municipality.getName()
        );
    }
}

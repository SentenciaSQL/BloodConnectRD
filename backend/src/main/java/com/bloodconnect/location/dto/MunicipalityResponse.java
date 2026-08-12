package com.bloodconnect.location.dto;

public record MunicipalityResponse(
        Long id,
        Long provinceId,
        String code,
        String name
) {
}

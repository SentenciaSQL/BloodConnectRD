package com.bloodconnect.mcp.mapper;

import com.bloodconnect.donationcenter.dto.DonationCenterResponse;
import com.bloodconnect.mcp.dto.McpDonationCenterDto;
import org.springframework.stereotype.Component;

@Component
public class McpDonationCenterMapper {

    public McpDonationCenterDto toPublicDto(DonationCenterResponse source) {
        return new McpDonationCenterDto(
                source.id(),
                source.name(),
                source.type(),
                source.provinceId(),
                source.provinceName(),
                source.municipalityId(),
                source.municipalityName(),
                source.sector(),
                source.address(),
                source.schedule(),
                source.phone(),
                source.latitude(),
                source.longitude(),
                source.approximateDistanceKm()
        );
    }
}

package com.bloodconnect.mcp.mapper;

import com.bloodconnect.bloodrequest.dto.BloodRequestResponse;
import com.bloodconnect.common.dto.PageResponse;
import com.bloodconnect.mcp.dto.McpBloodRequestDto;
import com.bloodconnect.mcp.dto.McpBloodRequestPageDto;
import org.springframework.stereotype.Component;

@Component
public class McpBloodRequestMapper {

    public McpBloodRequestDto toPublicDto(BloodRequestResponse source) {
        return new McpBloodRequestDto(
                source.id(),
                source.bloodType(),
                source.unitsRequired(),
                source.completedUnits(),
                source.pendingUnits(),
                source.progressPercent(),
                source.hospital(),
                source.provinceId(),
                source.provinceName(),
                source.municipalityId(),
                source.municipalityName(),
                source.sector(),
                source.latitude(),
                source.longitude(),
                source.deadline(),
                source.urgency(),
                source.status(),
                source.createdAt()
        );
    }

    public McpBloodRequestPageDto toPublicPage(PageResponse<BloodRequestResponse> page) {
        return new McpBloodRequestPageDto(
                page.content().stream().map(this::toPublicDto).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages(),
                page.first(),
                page.last()
        );
    }
}

package com.bloodconnect.mcp.dto;

import java.util.List;

public record McpBloodRequestPageDto(
        List<McpBloodRequestDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}

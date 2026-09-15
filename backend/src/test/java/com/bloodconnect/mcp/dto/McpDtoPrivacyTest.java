package com.bloodconnect.mcp.dto;

import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class McpDtoPrivacyTest {

    private static final Set<String> FORBIDDEN_FRAGMENTS = Set.of(
            "email",
            "phone",
            "token",
            "password",
            "document",
            "cedula",
            "passport",
            "patient",
            "contact"
    );

    @Test
    void mcpDtosDoNotExposeSensitiveFields() {
        assertNoSensitiveFields(McpBloodRequestDto.class);
        assertNoSensitiveFields(McpBloodRequestPageDto.class);
        assertNoSensitiveFields(McpDonationCenterDto.class);
        assertNoSensitiveFields(McpBloodCompatibilityDto.class);
        assertThat(fieldNames(McpDonationCenterDto.class)).contains("publicPhone", "publicAddress");
        assertThat(fieldNames(McpBloodRequestDto.class)).doesNotContain("address", "createdByName", "description");
    }

    private void assertNoSensitiveFields(Class<?> type) {
        for (String name : fieldNames(type)) {
            String normalized = name.toLowerCase(Locale.ROOT);
            for (String fragment : FORBIDDEN_FRAGMENTS) {
                if ("publicphone".equals(normalized) && "phone".equals(fragment)) {
                    continue;
                }
                assertThat(normalized)
                        .as("%s no debe exponer %s", type.getSimpleName(), fragment)
                        .doesNotContain(fragment);
            }
        }
    }

    private Set<String> fieldNames(Class<?> type) {
        return Arrays.stream(type.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(Collectors.toSet());
    }
}

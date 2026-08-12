package com.bloodconnect.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bloodconnect")
public record BloodConnectProperties(
        String country,
        String locale,
        String timezone,
        String dateFormat,
        String datetimeFormat
) {
}

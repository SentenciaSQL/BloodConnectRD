package com.bloodconnect.config;

import com.bloodconnect.security.jwt.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({BloodConnectProperties.class, JwtProperties.class})
public class AppConfig {
}

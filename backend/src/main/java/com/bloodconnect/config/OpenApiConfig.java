package com.bloodconnect.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI bloodConnectOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("BloodConnect RD API")
                        .description("API REST para conectar donantes de sangre con personas que necesitan sangre en República Dominicana.")
                        .version("v1")
                        .contact(new Contact()
                                .name("BloodConnect RD")
                                .email("soporte@bloodconnect.do"))
                        .license(new License().name("Uso educativo / portafolio")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Ingrese el access token JWT")));
    }
}

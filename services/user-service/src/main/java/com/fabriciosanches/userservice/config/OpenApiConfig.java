package com.fabriciosanches.userservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "User Service API",
                version = "v1",
                description = "Documentacao da API do servico de usuarios do EventMaster.",
                contact = @Contact(name = "EventMaster Team")
        ),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        description = "JWT Bearer token obtido no user-service ou via gateway. Informe apenas o token no botão Authorize.",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {

    @Bean
    public OpenApiCustomizer removeTrailingSlashDuplicatePaths() {
        return openApi -> {
            if (openApi.getPaths() == null || openApi.getPaths().isEmpty()) {
                return;
            }

            List<String> trailingSlashDuplicates = openApi.getPaths().keySet().stream()
                    .filter(path -> path.endsWith("/") && path.length() > 1)
                    .filter(path -> openApi.getPaths().containsKey(path.substring(0, path.length() - 1)))
                    .toList();

            trailingSlashDuplicates.forEach(path -> openApi.getPaths().remove(path));
        };
    }
}


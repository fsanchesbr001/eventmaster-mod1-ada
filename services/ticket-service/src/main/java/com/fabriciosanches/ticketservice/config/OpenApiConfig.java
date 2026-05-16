package com.fabriciosanches.ticketservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Ticket Service API",
                version = "v1",
                description = "API para cadastro, consulta, atualização e remoção de ingressos do EventMaster.",
                contact = @Contact(name = "EventMaster", email = "suporte@eventmaster.local"),
                license = @License(name = "Uso interno acadêmico")
        ),
        security = {
                @SecurityRequirement(name = "bearerAuth")
        },
        servers = {
                @Server(url = "http://localhost:8083", description = "Ambiente local do ticket-service")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Informe o JWT obtido no login do user-service"
)
public class OpenApiConfig {
}


package com.fabriciosanches.orderservice.config;

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
                title = "Order Service API",
                version = "v1",
                description = "API para criação de pedidos do EventMaster com integração síncrona e assíncrona.",
                contact = @Contact(name = "EventMaster", email = "suporte@eventmaster.local"),
                license = @License(name = "Uso interno acadêmico")
        ),
        security = @SecurityRequirement(name = "bearerAuth"),
        servers = @Server(url = "http://localhost:8084", description = "Ambiente local do order-service")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT Bearer token obtido no user-service ou via gateway. Informe apenas o token no botão Authorize."
)
public class OpenApiConfig {
}


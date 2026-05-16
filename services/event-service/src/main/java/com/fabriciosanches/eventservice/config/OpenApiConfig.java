package com.fabriciosanches.eventservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Event Service API",
                version = "v1",
                description = "API para cadastro, consulta, atualização e remoção de eventos do EventMaster.",
                contact = @Contact(name = "EventMaster", email = "suporte@eventmaster.local"),
                license = @License(name = "Uso interno acadêmico")
        ),
        servers = {
                @Server(url = "http://localhost:8082", description = "Ambiente local do event-service")
        }
)
public class OpenApiConfig {
}


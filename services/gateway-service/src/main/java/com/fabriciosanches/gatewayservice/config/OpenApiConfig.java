package com.fabriciosanches.gatewayservice.config;

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

    @Bean
    public OpenAPI gatewayOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("EventMaster Gateway Service API")
                        .description("Documentação do API Gateway do EventMaster, incluindo autenticação JWT, observabilidade e acesso às documentações downstream.")
                        .version("v1")
                        .contact(new Contact()
                                .name("EventMaster")
                                .email("suporte@eventmaster.local"))
                        .license(new License()
                                .name("Uso interno / acadêmico")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .schemaRequirement(securitySchemeName, new SecurityScheme()
                        .name(securitySchemeName)
                        .description("JWT Bearer token obtido no user-service ou via gateway. Informe apenas o token no botão Authorize.")
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"));
    }
}


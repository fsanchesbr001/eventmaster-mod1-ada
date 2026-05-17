package com.fabriciosanches.gatewayservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
public class SwaggerUiConfigController {

    @GetMapping({"/swagger-ui", "/swagger-ui/", "/swagger-ui/index.html"})
    public ResponseEntity<Void> swaggerUiAlias() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/swagger-ui.html"))
                .build();
    }

    @GetMapping(value = "/swagger-config.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> swaggerConfig(ServerHttpRequest request) {
        String baseUrl = request.getURI().getScheme() + "://" + request.getURI().getAuthority();

        return Map.of(
                "configUrl", "/swagger-config.json",
                "oauth2RedirectUrl", baseUrl + "/webjars/swagger-ui/oauth2-redirect.html",
                "urls", List.of(
                        Map.of("name", "gateway-service", "url", "/v3/api-docs"),
                        Map.of("name", "user-service via gateway", "url", "/api/users/v3/api-docs"),
                        Map.of("name", "event-service via gateway", "url", "/api/events/v3/api-docs"),
                        Map.of("name", "ticket-service via gateway", "url", "/api/tickets/v3/api-docs"),
                        Map.of("name", "order-service via gateway", "url", "/api/orders/v3/api-docs")
                ),
                "urls.primaryName", "gateway-service",
                "validatorUrl", ""
        );
    }
}


package com.fabriciosanches.gatewayservice.filter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class JwtValidationFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(JwtValidationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${api.security.token.secret}")
    private String jwtSecret;

    @Value("${api.security.token.issuer}")
    private String jwtIssuer;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (isPublicRoute(path) || HttpMethod.OPTIONS.equals(request.getMethod())) {
            return chain.filter(exchange);
        }

        String token = extractToken(request.getHeaders().getFirst(AUTHORIZATION_HEADER));
        if (token == null) {
            return writeUnauthorized(exchange, "Token ausente", "Informe o header Authorization com JWT Bearer.");
        }

        try {
            DecodedJWT decodedJWT = verifyToken(token);

            // Propaga dados ja validados para observabilidade/troubleshooting nos servicos.
            ServerHttpRequest requestWithAuthHeaders = request.mutate()
                    .header("X-Authenticated-User", decodedJWT.getSubject())
                    .header("X-Authenticated-Role", decodedJWT.getClaim("role").asString())
                    .build();

            return chain.filter(exchange.mutate().request(requestWithAuthHeaders).build());
        } catch (JWTVerificationException ex) {
            logger.warn("JWT invalido para path {}: {}", path, ex.getMessage());
            return writeUnauthorized(exchange, "Token invalido", "JWT invalido, expirado ou com assinatura incorreta.");
        }
    }

    private DecodedJWT verifyToken(String token) {
        Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
        JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer(jwtIssuer)
                .build();
        return verifier.verify(token);
    }

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }
        String value = authorizationHeader.trim();
        if (value.regionMatches(true, 0, "Bearer", 0, "Bearer".length())) {
            value = value.substring("Bearer".length()).trim();
        }
        return value.isBlank() ? null : value;
    }

    private boolean isPublicRoute(String path) {
        return path.startsWith("/api/auth/login")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/api/users/swagger-ui")
                || path.startsWith("/api/users/v3/api-docs")
                || path.startsWith("/api/events/swagger-ui")
                || path.startsWith("/api/events/v3/api-docs")
                || path.startsWith("/api/tickets/swagger-ui")
                || path.startsWith("/api/tickets/v3/api-docs")
                || path.startsWith("/api/orders/swagger-ui")
                || path.startsWith("/api/orders/v3/api-docs")
                || path.startsWith("/actuator/health")
                || path.startsWith("/actuator/info");
    }

    private Mono<Void> writeUnauthorized(ServerWebExchange exchange, String error, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        byte[] payload = toJsonBytes(Map.of(
                "error", error,
                "message", message
        ));

        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(payload);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private byte[] toJsonBytes(Map<String, String> payload) {
        try {
            return objectMapper.writeValueAsBytes(payload);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"Token invalido\",\"message\":\"Falha ao serializar resposta de erro\"}"
                    .getBytes(StandardCharsets.UTF_8);
        }
    }

    @Override
    public int getOrder() {
        // Executa antes do filtro de logging para que respostas 401 tambem fiquem registradas.
        return -2;
    }
}


package com.fabriciosanches.gatewayservice.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Iterator;
import java.util.Map;
import java.util.function.UnaryOperator;

@RestController
public class DownstreamOpenApiController {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${USER_SERVICE_URL:http://localhost:8080}")
    private String userServiceUrl;

    @Value("${EVENT_SERVICE_URL:http://localhost:8082}")
    private String eventServiceUrl;

    @Value("${TICKET_SERVICE_URL:http://localhost:8083}")
    private String ticketServiceUrl;

    @Value("${ORDER_SERVICE_URL:http://localhost:8084}")
    private String orderServiceUrl;

    public DownstreamOpenApiController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder().build();
    }

    @GetMapping(value = "/api/users/v3/api-docs", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<String>> userServiceOpenApi(ServerHttpRequest request) {
        return fetchAndRewrite(
                userServiceUrl + "/v3/api-docs",
                request,
                path -> {
                    if (path.startsWith("/auth/")) {
                        return "/api" + path;
                    }
                    if (path.startsWith("/user-service/usuarios")) {
                        return path.replaceFirst("^/user-service/usuarios", "/api/users");
                    }
                    return path;
                }
        );
    }

    @GetMapping(value = "/api/events/v3/api-docs", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<String>> eventServiceOpenApi(ServerHttpRequest request) {
        return fetchAndRewrite(
                eventServiceUrl + "/v3/api-docs",
                request,
                path -> path.replaceFirst("^/event-service/eventos", "/api/events")
        );
    }

    @GetMapping(value = "/api/tickets/v3/api-docs", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<String>> ticketServiceOpenApi(ServerHttpRequest request) {
        return fetchAndRewrite(
                ticketServiceUrl + "/v3/api-docs",
                request,
                path -> path.replaceFirst("^/ticket-service/ingressos", "/api/tickets")
        );
    }

    @GetMapping(value = "/api/orders/v3/api-docs", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<String>> orderServiceOpenApi(ServerHttpRequest request) {
        return fetchAndRewrite(
                orderServiceUrl + "/v3/api-docs",
                request,
                path -> path.replaceFirst("^/order-service/pedidos", "/api/orders")
        );
    }

    private Mono<ResponseEntity<String>> fetchAndRewrite(String downstreamUrl,
                                                         ServerHttpRequest request,
                                                         UnaryOperator<String> pathRewriter) {
        return webClient.get()
                .uri(downstreamUrl)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .map(body -> rewriteOpenApi(body, request, pathRewriter))
                .map(body -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body));
    }

    private String rewriteOpenApi(String rawJson,
                                  ServerHttpRequest request,
                                  UnaryOperator<String> pathRewriter) {
        try {
            ObjectNode root = (ObjectNode) objectMapper.readTree(rawJson);
            root.set("servers", buildGatewayServers(request));
            rewritePaths(root, pathRewriter);
            return objectMapper.writeValueAsString(root);
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao reescrever OpenAPI downstream", ex);
        }
    }

    private ArrayNode buildGatewayServers(ServerHttpRequest request) {
        String gatewayBaseUrl = request.getURI().getScheme() + "://" + request.getURI().getAuthority();

        ObjectNode gatewayServer = objectMapper.createObjectNode();
        gatewayServer.put("url", gatewayBaseUrl);
        gatewayServer.put("description", "API Gateway local");

        ArrayNode servers = objectMapper.createArrayNode();
        servers.add(gatewayServer);
        return servers;
    }

    private void rewritePaths(ObjectNode root, UnaryOperator<String> pathRewriter) {
        JsonNode pathsNode = root.get("paths");
        if (!(pathsNode instanceof ObjectNode pathsObject)) {
            return;
        }

        ObjectNode rewrittenPaths = objectMapper.createObjectNode();
        Iterator<Map.Entry<String, JsonNode>> fields = pathsObject.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            rewrittenPaths.set(pathRewriter.apply(entry.getKey()), entry.getValue());
        }

        root.set("paths", rewrittenPaths);
    }
}


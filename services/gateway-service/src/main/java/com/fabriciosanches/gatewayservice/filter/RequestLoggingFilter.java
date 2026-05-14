package com.fabriciosanches.gatewayservice.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;

@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        long startedAt = System.currentTimeMillis();

        ServerHttpRequest originalRequest = exchange.getRequest();
        String correlationId = Optional.ofNullable(originalRequest.getHeaders().getFirst(CORRELATION_ID_HEADER))
                .filter(value -> !value.isBlank())
                .orElseGet(() -> UUID.randomUUID().toString());

        ServerHttpRequest requestWithCorrelationId = originalRequest.mutate()
                .header(CORRELATION_ID_HEADER, correlationId)
                .build();

        exchange.getResponse().getHeaders().set(CORRELATION_ID_HEADER, correlationId);

        ServerWebExchange exchangeWithCorrelationId = exchange.mutate()
                .request(requestWithCorrelationId)
                .build();

        return chain.filter(exchangeWithCorrelationId)
                .doFinally(signalType -> {
                    long latencyMs = System.currentTimeMillis() - startedAt;
                    HttpStatusCode status = exchangeWithCorrelationId.getResponse().getStatusCode();
                    int statusCode = status != null ? status.value() : 0;

                    logger.info("gateway request method={} path={} status={} latencyMs={} correlationId={}",
                            requestWithCorrelationId.getMethod(),
                            requestWithCorrelationId.getURI().getPath(),
                            statusCode,
                            latencyMs,
                            correlationId);
                });
    }

    @Override
    public int getOrder() {
        return -1;
    }
}


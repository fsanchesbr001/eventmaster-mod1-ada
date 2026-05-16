package com.fabriciosanches.eventservice.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Value("${api.security.token.secret}")
    private String secret;

    @Value("${api.security.token.issuer:API Event Master}")
    private String issuer;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = extractToken(request.getHeader("Authorization"));
        if (token == null) {
            writeUnauthorized(response, "Token ausente", "Informe o header Authorization com JWT Bearer.");
            return;
        }

        try {
            var decodedJWT = JWT.require(Algorithm.HMAC256(secret))
                    .withIssuer(issuer)
                    .build()
                    .verify(token);

            String role = decodedJWT.getClaim("role").asString();
            if (role == null || role.isBlank()) {
                role = "ROLE_USER";
            }
            if (!role.startsWith("ROLE_")) {
                role = "ROLE_" + role;
            }

            var authentication = new UsernamePasswordAuthenticationToken(
                    decodedJWT.getSubject(),
                    token,
                    Collections.singletonList(new SimpleGrantedAuthority(role))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (JWTVerificationException ex) {
            logger.warn("Token JWT inválido no event-service: {}", ex.getMessage());
            writeUnauthorized(response, "Token invalido", "JWT invalido, expirado ou com assinatura incorreta.");
        }
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

    private void writeUnauthorized(HttpServletResponse response, String error, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\":\"" + error + "\",\"message\":\"" + message + "\"}");
    }
}


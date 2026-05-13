package com.fabriciosanches.userservice.security;

import java.time.OffsetDateTime;

public record DadosTokenJWT(
    String jwt,
    Long expirationMinutes,
    OffsetDateTime expiresAt,
    String usuarioLogin,
    String usuarioNome,
    String role
) {

    // Construtor compatível com código antigo que passa apenas jwt
    public DadosTokenJWT(String jwt) {
        this(jwt, null, null, null, null, null);
    }
}

package com.fabriciosanches.userservice.security;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(name = "DadosTokenJWT", description = "Resposta de autenticacao com token JWT e metadados da sessao")
public record DadosTokenJWT(
    @Schema(description = "Token JWT para autenticacao", example = "eyJhbGciOiJIUzI1NiJ9...") String jwt,
    @Schema(description = "Tempo de expiracao em minutos", example = "120") Long expirationMinutes,
    @Schema(description = "Instante de expiracao com timezone", example = "2026-05-14T12:30:00-03:00") OffsetDateTime expiresAt,
    @Schema(description = "Login/email do usuario autenticado", example = "admin@eventmaster.com") String usuarioLogin,
    @Schema(description = "Nome do usuario autenticado", example = "Administrador") String usuarioNome,
    @Schema(description = "Authority do usuario autenticado", example = "ROLE_ADMIN") String role
) {

    // Construtor compatível com código antigo que passa apenas jwt
    public DadosTokenJWT(String jwt) {
        this(jwt, null, null, null, null, null);
    }
}

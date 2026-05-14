package com.fabriciosanches.userservice.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AutenticacaoDTO", description = "Credenciais para autenticacao no user-service")
public record AutenticacaoDTO(
		@Schema(description = "Login/email do usuario", example = "admin@eventmaster.com") String login,
		@Schema(description = "Senha do usuario", example = "Senha@123") String senha
) {

}

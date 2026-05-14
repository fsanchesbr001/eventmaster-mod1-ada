package com.fabriciosanches.userservice.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "RoleOptionDTO", description = "Opcao de role para exibicao no frontend")
public record RoleOptionDTO(
		@Schema(description = "Valor tecnico da role", example = "ADMIN") String value,
		@Schema(description = "Descricao amigavel", example = "Administrador") String label,
		@Schema(description = "Chave de internacionalizacao", example = "role.ADMIN") String labelKey
) { }


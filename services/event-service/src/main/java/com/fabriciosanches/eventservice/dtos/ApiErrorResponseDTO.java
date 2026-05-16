package com.fabriciosanches.eventservice.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ApiErrorResponseDTO", description = "Resposta padrão de erro da API")
public record ApiErrorResponseDTO(
        @Schema(description = "Código resumido do erro", example = "BAD_REQUEST")
        String error,
        @Schema(description = "Mensagem detalhada para o cliente", example = "Nome do evento é obrigatório")
        String message
) {
}


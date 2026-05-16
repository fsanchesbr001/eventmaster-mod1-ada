package com.fabriciosanches.orderservice.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ApiErrorResponseDTO", description = "Formato padrão de erro do order-service")
public record ApiErrorResponseDTO(
        @Schema(description = "Código lógico do erro", example = "BAD_REQUEST")
        String error,
        @Schema(description = "Mensagem explicando a falha", example = "Pedido inválido")
        String message
) {
}


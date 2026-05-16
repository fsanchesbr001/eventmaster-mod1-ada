package com.fabriciosanches.orderservice.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "OrderItemRequestDTO", description = "Item do pedido contendo portador e tipo de ingresso")
public record OrderItemRequestDTO(
        @Schema(description = "Nome do portador do ingresso", example = "Fabricio Sanches")
        String nomePortador,
        @Schema(description = "CPF do portador", example = "123.456.789-00")
        String cpfPortador,
        @Schema(description = "Tipo do ingresso: INTEIRA ou MEIA", example = "MEIA")
        String tipoIngresso
) {
}


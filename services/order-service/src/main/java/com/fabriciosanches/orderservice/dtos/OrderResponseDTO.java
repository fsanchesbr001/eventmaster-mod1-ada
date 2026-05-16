package com.fabriciosanches.orderservice.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(name = "OrderResponseDTO", description = "Resumo do pedido criado")
public record OrderResponseDTO(
        @Schema(description = "Identificador do pedido", example = "1")
        Long id,
        @Schema(description = "Valor total calculado do pedido", example = "134.85")
        BigDecimal valorTotal,
        @Schema(description = "Status do pedido", example = "REALIZADO")
        String status
) {
}


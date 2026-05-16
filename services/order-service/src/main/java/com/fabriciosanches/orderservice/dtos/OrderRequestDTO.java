package com.fabriciosanches.orderservice.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "OrderRequestDTO", description = "Payload de criação de um pedido")
public record OrderRequestDTO(
        @Schema(description = "Identificador do evento", example = "1")
        Long eventId,
        @Schema(description = "Forma de pagamento: PIX, CARTAO ou BOLETO", example = "PIX")
        String formaPagamento,
        @Schema(description = "Itens do pedido")
        List<OrderItemRequestDTO> itens
) {
}


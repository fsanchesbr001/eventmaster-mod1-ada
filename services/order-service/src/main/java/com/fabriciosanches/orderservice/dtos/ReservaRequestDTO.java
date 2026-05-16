package com.fabriciosanches.orderservice.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ReservaRequestDTO", description = "Solicitação síncrona de reserva de posições no Redis do ticket-service")
public record ReservaRequestDTO(
        @Schema(description = "Identificador do evento", example = "1")
        Long eventId,
        @Schema(description = "Quantidade de ingressos a reservar", example = "2")
        Integer quantidade
) {
}


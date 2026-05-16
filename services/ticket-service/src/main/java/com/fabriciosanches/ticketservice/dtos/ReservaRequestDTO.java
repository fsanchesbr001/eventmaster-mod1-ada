package com.fabriciosanches.ticketservice.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ReservaRequestDTO", description = "Solicitação de reserva síncrona de ingressos no Redis")
public record ReservaRequestDTO(
        @Schema(description = "Identificador do evento", example = "1")
        Long eventId,
        @Schema(description = "Quantidade de posições a reservar", example = "2")
        Integer quantidade
) {
}


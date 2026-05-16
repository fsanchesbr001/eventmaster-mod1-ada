package com.fabriciosanches.orderservice.dtos;

import java.math.BigDecimal;

public record PedidoRealizadoEvent(
        Long pedidoId,
        String usuarioId,
        Long eventoId,
        BigDecimal valorTotal
) {
}


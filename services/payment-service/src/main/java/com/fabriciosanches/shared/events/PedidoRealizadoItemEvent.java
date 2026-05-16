package com.fabriciosanches.shared.events;

import java.math.BigDecimal;

public record PedidoRealizadoItemEvent(
        String nomePortador,
        String cpfPortador,
        String tipoIngresso,
        BigDecimal precoPago
) {
}


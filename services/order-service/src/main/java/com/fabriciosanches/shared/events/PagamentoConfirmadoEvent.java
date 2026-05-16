package com.fabriciosanches.shared.events;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record PagamentoConfirmadoEvent(
        Long pedidoId,
        String usuarioId,
        Long eventoId,
        String eventoNome,
        LocalDate dataEvento,
        LocalTime horaEvento,
        BigDecimal valorTotal,
        List<PedidoRealizadoItemEvent> itens
) {
}


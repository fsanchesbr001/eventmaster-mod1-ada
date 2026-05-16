package com.fabriciosanches.orderservice.listener;

import com.fabriciosanches.orderservice.service.OrderService;
import com.fabriciosanches.shared.events.PagamentoNegadoEvent;
import com.fabriciosanches.shared.events.PedidoRealizadoItemEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PagamentoNegadoListenerTests {

    @Mock
    private OrderService orderService;

    private PagamentoNegadoListener listener;

    @BeforeEach
    void setUp() {
        listener = new PagamentoNegadoListener(orderService, "PAGAMENTO_NEGADO");
    }

    @Test
    void shouldDelegateDeniedPaymentToOrderService() {
        PagamentoNegadoEvent event = new PagamentoNegadoEvent(
                9L,
                "usuario-1",
                10L,
                "Show da ADA",
                LocalDate.of(2026, 12, 10),
                LocalTime.of(20, 0),
                new BigDecimal("150.00"),
                List.of(new PedidoRealizadoItemEvent("Fabricio Sanches", "123.456.789-00", "INTEIRA", new BigDecimal("100.00")))
        );

        listener.consumir(event);

        verify(orderService).processarPagamentoNegado(event);
    }
}


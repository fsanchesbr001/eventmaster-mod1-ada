package com.fabriciosanches.paymentservice.listener;

import com.fabriciosanches.paymentservice.service.PaymentProcessorService;
import com.fabriciosanches.shared.events.PedidoRealizadoEvent;
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
class PedidoRealizadoListenerTests {

    @Mock
    private PaymentProcessorService paymentProcessorService;

    private PedidoRealizadoListener listener;

    @BeforeEach
    void setUp() {
        listener = new PedidoRealizadoListener(paymentProcessorService, "PEDIDO_REALIZADO");
    }

    @Test
    void shouldDelegatePedidoRealizadoToPaymentProcessor() {
        PedidoRealizadoEvent event = new PedidoRealizadoEvent(
                1L,
                "usuario-1",
                10L,
                "Show da ADA",
                LocalDate.of(2026, 12, 10),
                LocalTime.of(20, 0),
                new BigDecimal("150.00"),
                List.of(new PedidoRealizadoItemEvent("Fabricio Sanches", "123.456.789-00", "INTEIRA", new BigDecimal("100.00")))
        );

        listener.consumir(event);

        verify(paymentProcessorService).processar(event);
    }
}


package com.fabriciosanches.ticketservice.listener;

import com.fabriciosanches.shared.events.PedidoCanceladoEvent;
import com.fabriciosanches.shared.events.PedidoRealizadoItemEvent;
import com.fabriciosanches.ticketservice.config.TicketKafkaTopicsProperties;
import com.fabriciosanches.ticketservice.service.TicketService;
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
class PedidoCanceladoListenerTests {

    @Mock
    private TicketService ticketService;

    private PedidoCanceladoListener listener;

    @BeforeEach
    void setUp() {
        TicketKafkaTopicsProperties kafkaTopicsProperties = new TicketKafkaTopicsProperties();
        kafkaTopicsProperties.setPedidoCancelado("PEDIDO_CANCELADO");
        listener = new PedidoCanceladoListener(ticketService, kafkaTopicsProperties);
    }

    @Test
    void shouldDelegateCancelledOrderToTicketService() {
        PedidoCanceladoEvent event = new PedidoCanceladoEvent(
                9L,
                "usuario-1",
                10L,
                "Show da ADA",
                LocalDate.of(2026, 12, 10),
                LocalTime.of(20, 0),
                new BigDecimal("100.00"),
                List.of(new PedidoRealizadoItemEvent("Fabricio Sanches", "123.456.789-00", "INTEIRA", new BigDecimal("100.00")))
        );

        listener.consumir(event);

        verify(ticketService).cancelarPedido(event);
    }
}


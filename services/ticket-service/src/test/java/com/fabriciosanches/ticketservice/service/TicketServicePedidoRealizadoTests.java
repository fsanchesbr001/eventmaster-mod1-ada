package com.fabriciosanches.ticketservice.service;

import com.fabriciosanches.shared.events.PedidoRealizadoEvent;
import com.fabriciosanches.shared.events.PedidoRealizadoItemEvent;
import com.fabriciosanches.ticketservice.domain.Ticket;
import com.fabriciosanches.ticketservice.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServicePedidoRealizadoTests {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private EventInventoryService eventInventoryService;

    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketService(ticketRepository, eventInventoryService);
    }

    @Test
    void shouldPersistPhysicalTicketsFromPedidoRealizadoEvent() {
        PedidoRealizadoEvent event = new PedidoRealizadoEvent(
                1L,
                "usuario-1",
                10L,
                "Show da ADA",
                LocalDate.of(2026, 12, 10),
                LocalTime.of(20, 0, 0),
                new BigDecimal("150.00"),
                List.of(
                        new PedidoRealizadoItemEvent("Fabricio Sanches", "123.456.789-00", "INTEIRA", new BigDecimal("100.00")),
                        new PedidoRealizadoItemEvent("Ada Lovelace", "987.654.321-00", "MEIA", new BigDecimal("50.00"))
                )
        );

        when(ticketRepository.existsByPedidoId(1L)).thenReturn(false);
        when(ticketRepository.findTopByOrderByIdIngressoDesc()).thenReturn(Optional.empty());
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ticketService.registrarPedidoRealizado(event);

        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository, times(2)).save(captor.capture());

        List<Ticket> tickets = captor.getAllValues();
        assertEquals(1L, tickets.getFirst().getIdIngresso());
        assertEquals("Show da ADA", tickets.getFirst().getEvento());
        assertEquals(1L, tickets.getFirst().getPedidoId());
        assertEquals("Inteira", tickets.getFirst().getTipoIngresso());
        assertEquals(new BigDecimal("100.00"), tickets.getFirst().getValor());
        assertEquals("Reservado", tickets.getFirst().getSituacao());
        assertEquals(2L, tickets.get(1).getIdIngresso());
        assertEquals(1L, tickets.get(1).getPedidoId());
        assertEquals("Meia", tickets.get(1).getTipoIngresso());
        assertEquals(new BigDecimal("50.00"), tickets.get(1).getValor());
    }

    @Test
    void shouldIgnoreDuplicatePedidoRealizadoDelivery() {
        PedidoRealizadoEvent event = new PedidoRealizadoEvent(
                1L,
                "usuario-1",
                10L,
                "Show da ADA",
                LocalDate.of(2026, 12, 10),
                LocalTime.of(20, 0, 0),
                new BigDecimal("150.00"),
                List.of(
                        new PedidoRealizadoItemEvent("Fabricio Sanches", "123.456.789-00", "INTEIRA", new BigDecimal("100.00"))
                )
        );

        when(ticketRepository.existsByPedidoId(1L)).thenReturn(true);

        ticketService.registrarPedidoRealizado(event);

        verify(ticketRepository).existsByPedidoId(1L);
        verify(ticketRepository, times(0)).findTopByOrderByIdIngressoDesc();
        verify(ticketRepository, times(0)).save(any(Ticket.class));
    }
}


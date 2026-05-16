package com.fabriciosanches.ticketservice.service;

import com.fabriciosanches.shared.events.PedidoCanceladoEvent;
import com.fabriciosanches.shared.events.PedidoConfirmadoEvent;
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
import static org.mockito.Mockito.never;
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

    @Test
    void shouldConfirmReservedTicketsWhenPedidoConfirmadoArrives() {
        Ticket reservado = criarTicket(1L, 1L, "Reservado");
        when(ticketRepository.findByPedidoId(1L)).thenReturn(List.of(reservado));

        ticketService.confirmarPedido(criarPedidoConfirmadoEvent(1L));

        assertEquals("Confirmado", reservado.getSituacao());
        verify(ticketRepository).saveAll(List.of(reservado));
    }

    @Test
    void shouldCancelReservedTicketsAndReturnStockWhenPedidoCanceladoArrives() {
        Ticket reservado = criarTicket(1L, 9L, "Reservado");
        when(ticketRepository.findByPedidoId(9L)).thenReturn(List.of(reservado));

        ticketService.cancelarPedido(criarPedidoCanceladoEvent(9L, 10L));

        assertEquals("Disponivel", reservado.getSituacao());
        verify(eventInventoryService).devolverIngressos(10L, 1);
        verify(ticketRepository).saveAll(List.of(reservado));
    }

    @Test
    void shouldMaterializeConfirmedTicketsWhenFinalEventArrivesBeforePedidoRealizado() {
        when(ticketRepository.findByPedidoId(1L)).thenReturn(List.of());
        when(ticketRepository.findTopByOrderByIdIngressoDesc()).thenReturn(Optional.empty());
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ticketService.confirmarPedido(criarPedidoConfirmadoEvent(1L));

        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(captor.capture());
        assertEquals("Confirmado", captor.getValue().getSituacao());
        verify(eventInventoryService, never()).devolverIngressos(any(), any());
    }

    @Test
    void shouldMaterializeAvailableTicketsAndReturnStockWhenCancellationArrivesBeforePedidoRealizado() {
        when(ticketRepository.findByPedidoId(9L)).thenReturn(List.of());
        when(ticketRepository.findTopByOrderByIdIngressoDesc()).thenReturn(Optional.empty());
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ticketService.cancelarPedido(criarPedidoCanceladoEvent(9L, 10L));

        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(captor.capture());
        assertEquals("Disponivel", captor.getValue().getSituacao());
        verify(eventInventoryService).devolverIngressos(10L, 1);
    }

    @Test
    void shouldIgnoreDuplicatePedidoCanceladoWhenTicketsAreAlreadyAvailable() {
        Ticket disponivel = criarTicket(1L, 9L, "Disponivel");
        when(ticketRepository.findByPedidoId(9L)).thenReturn(List.of(disponivel));

        ticketService.cancelarPedido(criarPedidoCanceladoEvent(9L, 10L));

        verify(eventInventoryService, never()).devolverIngressos(any(), any());
        verify(ticketRepository, never()).saveAll(any());
    }

    private Ticket criarTicket(Long idIngresso, Long pedidoId, String situacao) {
        Ticket ticket = new Ticket(
                idIngresso,
                "Show da ADA",
                LocalDate.of(2026, 12, 10),
                LocalTime.of(20, 0),
                "Inteira",
                new BigDecimal("100.00"),
                "Fabricio Sanches",
                "123.456.789-00",
                situacao
        );
        ticket.setPedidoId(pedidoId);
        return ticket;
    }

    private PedidoConfirmadoEvent criarPedidoConfirmadoEvent(Long pedidoId) {
        return new PedidoConfirmadoEvent(
                pedidoId,
                "usuario-1",
                10L,
                "Show da ADA",
                LocalDate.of(2026, 12, 10),
                LocalTime.of(20, 0),
                new BigDecimal("100.00"),
                List.of(new PedidoRealizadoItemEvent("Fabricio Sanches", "123.456.789-00", "INTEIRA", new BigDecimal("100.00")))
        );
    }

    private PedidoCanceladoEvent criarPedidoCanceladoEvent(Long pedidoId, Long eventoId) {
        return new PedidoCanceladoEvent(
                pedidoId,
                "usuario-1",
                eventoId,
                "Show da ADA",
                LocalDate.of(2026, 12, 10),
                LocalTime.of(20, 0),
                new BigDecimal("100.00"),
                List.of(new PedidoRealizadoItemEvent("Fabricio Sanches", "123.456.789-00", "INTEIRA", new BigDecimal("100.00")))
        );
    }
}


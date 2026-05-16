package com.fabriciosanches.ticketservice;

import com.fabriciosanches.ticketservice.controller.TicketController;
import com.fabriciosanches.ticketservice.dtos.TicketRequestDTO;
import com.fabriciosanches.ticketservice.dtos.TicketResponseDTO;
import com.fabriciosanches.ticketservice.exceptions.TicketNotFoundException;
import com.fabriciosanches.ticketservice.exceptions.TicketValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class TicketControllerIntegrationTests {

    @Autowired
    private TicketController ticketController;

    @Test
    void shouldExecuteCrudFlowSuccessfully() {
        TicketRequestDTO createRequest = new TicketRequestDTO(
                100001L,
                "Show da ADA",
                LocalDate.of(2026, 12, 10),
                LocalTime.of(20, 0, 0),
                "Inteira",
                new BigDecimal("120.50"),
                "Fabricio Sanches",
                "123.456.789-00",
                null
        );

        TicketResponseDTO created = ticketController.criar(createRequest).getBody();
        assertNotNull(created);
        assertNotNull(created.id());
        assertEquals(100001L, created.idIngresso());
        assertEquals("Show da ADA", created.evento());
        assertEquals("Disponivel", created.situacao());

        TicketResponseDTO found = ticketController.buscarPorId(created.id()).getBody();
        assertNotNull(found);
        assertEquals(created.id(), found.id());
        assertEquals("Inteira", found.tipoIngresso());
        assertEquals("Disponivel", found.situacao());

        assertFalse(ticketController.listarTodos().getBody().isEmpty());

        TicketRequestDTO updateRequest = new TicketRequestDTO(
                100001L,
                "Peça de Teatro Premium",
                LocalDate.of(2026, 12, 12),
                LocalTime.of(21, 30, 0),
                "meia",
                new BigDecimal("75.00"),
                "Fabricio Sanches",
                "123.456.789-00",
                "Reservado"
        );

        TicketResponseDTO updated = ticketController.atualizar(created.id(), updateRequest).getBody();
        assertNotNull(updated);
        assertEquals("Peça de Teatro Premium", updated.evento());
        assertEquals("Meia", updated.tipoIngresso());
        assertEquals(new BigDecimal("75.00"), updated.valor());
        assertEquals("Reservado", updated.situacao());

        assertEquals(204, ticketController.excluir(created.id()).getStatusCode().value());

        assertThrows(TicketNotFoundException.class, () -> ticketController.buscarPorId(created.id()));
    }

    @Test
    void shouldRejectInvalidPayload() {
        TicketRequestDTO invalidRequest = new TicketRequestDTO(
                null,
                "",
                LocalDate.of(2026, 12, 10),
                LocalTime.of(20, 0, 0),
                "VIP",
                BigDecimal.ZERO,
                "",
                "",
                ""
        );

        assertThrows(TicketValidationException.class, () -> ticketController.criar(invalidRequest));
    }

    @Test
    void shouldRejectSituacaoWithMoreThanTwentyCharacters() {
        TicketRequestDTO invalidRequest = new TicketRequestDTO(
                100002L,
                "Show da ADA",
                LocalDate.of(2026, 12, 10),
                LocalTime.of(20, 0, 0),
                "Inteira",
                new BigDecimal("50.00"),
                "Fabricio Sanches",
                "123.456.789-00",
                "SituacaoMuitoMaiorQueVinte"
        );

        assertThrows(TicketValidationException.class, () -> ticketController.criar(invalidRequest));
    }
}


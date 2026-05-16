package com.fabriciosanches.eventservice;

import com.fabriciosanches.eventservice.controller.EventControler;
import com.fabriciosanches.eventservice.dtos.EventRequestDTO;
import com.fabriciosanches.eventservice.dtos.EventResponseDTO;
import com.fabriciosanches.eventservice.exceptions.EventNotFoundException;
import com.fabriciosanches.eventservice.exceptions.EventValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class EventControlerIntegrationTests {

    @Autowired
    private EventControler eventControler;

    @Test
    void shouldExecuteCrudFlowSuccessfully() {
        EventRequestDTO createRequest = new EventRequestDTO(
                "Show da ADA",
                LocalDate.of(2026, 12, 10),
                LocalTime.of(20, 0, 0),
                "Teatro Central",
                1500.0,
                new BigDecimal("89.90")
        );

        EventResponseDTO created = eventControler.criar(createRequest).getBody();
        assertNotNull(created);
        assertNotNull(created.id());
        assertEquals("Show da ADA", created.nome());
        assertEquals(new BigDecimal("89.90"), created.precoBase());

        EventResponseDTO found = eventControler.buscarPorId(created.id()).getBody();
        assertNotNull(found);
        assertEquals(created.id(), found.id());
        assertEquals("Teatro Central", found.local());
        assertEquals(new BigDecimal("89.90"), found.precoBase());

        List<EventResponseDTO> eventos = eventControler.listarTodos().getBody();
        assertNotNull(eventos);
        assertFalse(eventos.isEmpty());

        EventRequestDTO updateRequest = new EventRequestDTO(
                "Peça de Teatro Premium",
                LocalDate.of(2026, 12, 12),
                LocalTime.of(21, 30, 0),
                "Auditório Principal",
                950.0,
                new BigDecimal("120.00")
        );

        EventResponseDTO updated = eventControler.atualizar(created.id(), updateRequest).getBody();
        assertNotNull(updated);
        assertEquals("Peça de Teatro Premium", updated.nome());
        assertEquals(950.0, updated.capacidade());
        assertEquals(new BigDecimal("120.00"), updated.precoBase());

        assertEquals(204, eventControler.excluir(created.id()).getStatusCode().value());

        assertThrows(EventNotFoundException.class, () -> eventControler.buscarPorId(created.id()));
    }

    @Test
    void shouldRejectInvalidPayload() {
        EventRequestDTO invalidRequest = new EventRequestDTO(
                "",
                LocalDate.of(2026, 12, 10),
                LocalTime.of(20, 0, 0),
                "Arena",
                0.0,
                new BigDecimal("-1.00")
        );

        assertThrows(EventValidationException.class, () -> eventControler.criar(invalidRequest));
    }

    @Test
    void shouldDefaultPrecoBaseToZeroWhenNotProvided() {
        EventRequestDTO createRequest = new EventRequestDTO(
                "Workshop ADA",
                LocalDate.of(2026, 11, 5),
                LocalTime.of(9, 0, 0),
                "Sala 1",
                100.0,
                null
        );

        EventResponseDTO created = eventControler.criar(createRequest).getBody();

        assertNotNull(created);
        assertEquals(new BigDecimal("0.00"), created.precoBase());
    }
}


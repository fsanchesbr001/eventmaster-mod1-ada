package com.fabriciosanches.eventservice.service;

import com.fabriciosanches.eventservice.domain.Event;
import com.fabriciosanches.eventservice.dtos.EventRequestDTO;
import com.fabriciosanches.eventservice.repository.EventRepository;
import com.fabriciosanches.shared.events.EventCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceMessagingTests {

	@Mock
	private EventRepository eventRepository;

	@Mock
	private KafkaTemplate<String, EventCreatedEvent> kafkaTemplate;

	private EventService eventService;

	@BeforeEach
	void setUp() {
		eventService = new EventService(eventRepository, kafkaTemplate, "EVENTO_CRIADO", true);
	}

	@Test
	void shouldPublishEventoCriadoAfterPersistingEvent() {
		EventRequestDTO request = new EventRequestDTO(
				"Show da ADA",
				LocalDate.of(2026, 12, 10),
				LocalTime.of(20, 0, 0),
				"Teatro Central",
				1500.0,
				new BigDecimal("89.90")
		);

		when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
			Event event = invocation.getArgument(0);
			event.setId(1L);
			return event;
		});

		eventService.criar(request);

		verify(kafkaTemplate).send(eq("EVENTO_CRIADO"), eq("1"), argThat(matchesEventCreated(1L, "Show da ADA", 1500)));
	}

	@Test
	void shouldNotPublishWhenMessagingIsDisabled() {
		EventService disabledEventService = new EventService(eventRepository, kafkaTemplate, "EVENTO_CRIADO", false);
		EventRequestDTO request = new EventRequestDTO(
				"Show da ADA",
				LocalDate.of(2026, 12, 10),
				LocalTime.of(20, 0, 0),
				"Teatro Central",
				1500.0,
				new BigDecimal("89.90")
		);

		when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
			Event event = invocation.getArgument(0);
			event.setId(2L);
			return event;
		});

		disabledEventService.criar(request);

		verify(kafkaTemplate, never()).send(any(), any(), any());
	}

	private ArgumentMatcher<EventCreatedEvent> matchesEventCreated(Long eventId, String nome, Integer capacidade) {
		return event -> event != null
				&& eventId.equals(event.eventId())
				&& nome.equals(event.nome())
				&& capacidade.equals(event.capacidade());
	}
}


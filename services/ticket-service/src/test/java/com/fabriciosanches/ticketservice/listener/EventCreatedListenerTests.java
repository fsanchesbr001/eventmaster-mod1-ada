package com.fabriciosanches.ticketservice.listener;

import com.fabriciosanches.shared.events.EventCreatedEvent;
import com.fabriciosanches.ticketservice.config.TicketKafkaTopicsProperties;
import com.fabriciosanches.ticketservice.service.EventInventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EventCreatedListenerTests {

    @Mock
    private EventInventoryService eventInventoryService;

    private EventCreatedListener listener;

    @BeforeEach
    void setUp() {
        TicketKafkaTopicsProperties kafkaTopicsProperties = new TicketKafkaTopicsProperties();
        kafkaTopicsProperties.setEventoCriado("EVENTO_CRIADO");
        listener = new EventCreatedListener(eventInventoryService, kafkaTopicsProperties);
    }

    @Test
    void shouldPreloadRedisWithNinetyFivePercentOfCapacity() {
        EventCreatedEvent event = new EventCreatedEvent(1L, "Show da ADA", 101);
        when(eventInventoryService.buildInventoryKey(1L)).thenReturn("evento:1:ingressos_disponiveis");

        listener.consumir(event);

        verify(eventInventoryService).carregarEstoqueInicial(1L, 101);
    }
}


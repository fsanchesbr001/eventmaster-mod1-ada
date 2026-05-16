package com.fabriciosanches.ticketservice.listener;

import com.fabriciosanches.shared.events.EventCreatedEvent;
import com.fabriciosanches.ticketservice.config.TicketKafkaTopicsProperties;
import com.fabriciosanches.ticketservice.service.EventInventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class EventCreatedListener {

    private static final Logger logger = LoggerFactory.getLogger(EventCreatedListener.class);

    private final EventInventoryService eventInventoryService;
    private final String eventoCriadoTopic;

    public EventCreatedListener(EventInventoryService eventInventoryService,
                                TicketKafkaTopicsProperties kafkaTopicsProperties) {
        this.eventInventoryService = eventInventoryService;
        this.eventoCriadoTopic = kafkaTopicsProperties.getEventoCriado();
    }

    @KafkaListener(topics = "${app.kafka.topic.evento-criado}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumir(EventCreatedEvent evento) {
        logger.info("Recebido {} para eventId={} nome='{}' capacidade={}", eventoCriadoTopic, evento.eventId(), evento.nome(), evento.capacidade());

        eventInventoryService.carregarEstoqueInicial(evento.eventId(), evento.capacidade());

        logger.info("Carga efetuada no Redis para a chave {}", eventInventoryService.buildInventoryKey(evento.eventId()));
    }
}


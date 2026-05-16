package com.fabriciosanches.ticketservice.listener;

import com.fabriciosanches.shared.events.PedidoRealizadoEvent;
import com.fabriciosanches.ticketservice.config.TicketKafkaTopicsProperties;
import com.fabriciosanches.ticketservice.service.TicketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PedidoRealizadoListener {

    private static final Logger logger = LoggerFactory.getLogger(PedidoRealizadoListener.class);

    private final TicketService ticketService;
    private final String pedidoRealizadoTopic;

    public PedidoRealizadoListener(TicketService ticketService,
                                   TicketKafkaTopicsProperties kafkaTopicsProperties) {
        this.ticketService = ticketService;
        this.pedidoRealizadoTopic = kafkaTopicsProperties.getPedidoRealizado();
    }

    @KafkaListener(topics = "${app.kafka.topic.pedido-realizado}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumir(PedidoRealizadoEvent event) {
        logger.info("Recebido {} para pedidoId={} eventoId={} com {} itens",
                pedidoRealizadoTopic,
                event.pedidoId(),
                event.eventoId(),
                event.itens() == null ? 0 : event.itens().size());

        ticketService.registrarPedidoRealizado(event);
    }
}


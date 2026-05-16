package com.fabriciosanches.ticketservice.listener;

import com.fabriciosanches.shared.events.PedidoCanceladoEvent;
import com.fabriciosanches.ticketservice.config.TicketKafkaTopicsProperties;
import com.fabriciosanches.ticketservice.service.TicketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PedidoCanceladoListener {

    private static final Logger logger = LoggerFactory.getLogger(PedidoCanceladoListener.class);

    private final TicketService ticketService;
    private final String pedidoCanceladoTopic;

    public PedidoCanceladoListener(TicketService ticketService,
                                   TicketKafkaTopicsProperties kafkaTopicsProperties) {
        this.ticketService = ticketService;
        this.pedidoCanceladoTopic = kafkaTopicsProperties.getPedidoCancelado();
    }

    @KafkaListener(topics = "${app.kafka.topic.pedido-cancelado}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumir(PedidoCanceladoEvent event) {
        logger.info("Recebido {} para pedidoId={} eventoId={} com {} itens",
                pedidoCanceladoTopic,
                event.pedidoId(),
                event.eventoId(),
                event.itens() == null ? 0 : event.itens().size());

        ticketService.cancelarPedido(event);
    }
}


package com.fabriciosanches.paymentservice.listener;

import com.fabriciosanches.paymentservice.service.PaymentProcessorService;
import com.fabriciosanches.shared.events.PedidoRealizadoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PedidoRealizadoListener {

    private static final Logger logger = LoggerFactory.getLogger(PedidoRealizadoListener.class);

    private final PaymentProcessorService paymentProcessorService;
    private final String pedidoRealizadoTopic;

    public PedidoRealizadoListener(PaymentProcessorService paymentProcessorService,
                                   @Value("${app.kafka.topic.pedido-realizado}") String pedidoRealizadoTopic) {
        this.paymentProcessorService = paymentProcessorService;
        this.pedidoRealizadoTopic = pedidoRealizadoTopic;
    }

    @KafkaListener(topics = "${app.kafka.topic.pedido-realizado}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumir(PedidoRealizadoEvent event) {
        logger.info("Recebido {} para pedidoId={} eventoId={} com {} itens",
                pedidoRealizadoTopic,
                event.pedidoId(),
                event.eventoId(),
                event.itens() == null ? 0 : event.itens().size());

        paymentProcessorService.processar(event);
    }
}


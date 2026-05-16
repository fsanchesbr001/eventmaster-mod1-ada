package com.fabriciosanches.orderservice.listener;

import com.fabriciosanches.orderservice.service.OrderService;
import com.fabriciosanches.shared.events.PagamentoNegadoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PagamentoNegadoListener {

    private static final Logger logger = LoggerFactory.getLogger(PagamentoNegadoListener.class);

    private final OrderService orderService;
    private final String pagamentoNegadoTopic;

    public PagamentoNegadoListener(OrderService orderService,
                                   @Value("${app.kafka.topic.pagamento-negado}") String pagamentoNegadoTopic) {
        this.orderService = orderService;
        this.pagamentoNegadoTopic = pagamentoNegadoTopic;
    }

    @KafkaListener(topics = "${app.kafka.topic.pagamento-negado}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumir(PagamentoNegadoEvent event) {
        logger.info("Recebido {} para pedidoId={} eventoId={} com {} itens",
                pagamentoNegadoTopic,
                event.pedidoId(),
                event.eventoId(),
                event.itens() == null ? 0 : event.itens().size());

        orderService.processarPagamentoNegado(event);
    }
}


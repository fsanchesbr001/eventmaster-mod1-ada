package com.fabriciosanches.orderservice.listener;

import com.fabriciosanches.orderservice.service.OrderService;
import com.fabriciosanches.shared.events.PagamentoConfirmadoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PagamentoConfirmadoListener {

    private static final Logger logger = LoggerFactory.getLogger(PagamentoConfirmadoListener.class);

    private final OrderService orderService;
    private final String pagamentoConfirmadoTopic;

    public PagamentoConfirmadoListener(OrderService orderService,
                                       @Value("${app.kafka.topic.pagamento-confirmado}") String pagamentoConfirmadoTopic) {
        this.orderService = orderService;
        this.pagamentoConfirmadoTopic = pagamentoConfirmadoTopic;
    }

    @KafkaListener(topics = "${app.kafka.topic.pagamento-confirmado}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumir(PagamentoConfirmadoEvent event) {
        logger.info("Recebido {} para pedidoId={} eventoId={} com {} itens",
                pagamentoConfirmadoTopic,
                event.pedidoId(),
                event.eventoId(),
                event.itens() == null ? 0 : event.itens().size());

        orderService.processarPagamentoConfirmado(event);
    }
}


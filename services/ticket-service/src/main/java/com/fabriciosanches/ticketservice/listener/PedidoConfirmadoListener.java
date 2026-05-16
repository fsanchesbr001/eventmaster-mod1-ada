package com.fabriciosanches.ticketservice.listener;

import com.fabriciosanches.shared.events.PedidoConfirmadoEvent;
import com.fabriciosanches.ticketservice.config.TicketKafkaTopicsProperties;
import com.fabriciosanches.ticketservice.service.TicketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PedidoConfirmadoListener {

	private static final Logger logger = LoggerFactory.getLogger(PedidoConfirmadoListener.class);

	private final TicketService ticketService;
	private final String pedidoConfirmadoTopic;

	public PedidoConfirmadoListener(TicketService ticketService,
									TicketKafkaTopicsProperties kafkaTopicsProperties) {
		this.ticketService = ticketService;
		this.pedidoConfirmadoTopic = kafkaTopicsProperties.getPedidoConfirmado();
	}

	@KafkaListener(topics = "${app.kafka.topic.pedido-confirmado}", groupId = "${spring.kafka.consumer.group-id}")
	public void consumir(PedidoConfirmadoEvent event) {
		logger.info("Recebido {} para pedidoId={} eventoId={} com {} itens",
				pedidoConfirmadoTopic,
				event.pedidoId(),
				event.eventoId(),
				event.itens() == null ? 0 : event.itens().size());

		ticketService.confirmarPedido(event);
	}
}


package com.fabriciosanches.paymentservice.service;

import com.fabriciosanches.shared.events.PagamentoConfirmadoEvent;
import com.fabriciosanches.shared.events.PagamentoNegadoEvent;
import com.fabriciosanches.shared.events.PedidoRealizadoEvent;
import com.fabriciosanches.shared.events.PedidoRealizadoItemEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class PaymentProcessorService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentProcessorService.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String pagamentoConfirmadoTopic;
    private final String pagamentoNegadoTopic;

    public PaymentProcessorService(KafkaTemplate<String, Object> kafkaTemplate,
                                   @Value("${app.kafka.topic.pagamento-confirmado}") String pagamentoConfirmadoTopic,
                                   @Value("${app.kafka.topic.pagamento-negado}") String pagamentoNegadoTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.pagamentoConfirmadoTopic = pagamentoConfirmadoTopic;
        this.pagamentoNegadoTopic = pagamentoNegadoTopic;
    }

    public void processar(PedidoRealizadoEvent event) {
        validarPedidoRealizado(event);

        if (pagamentoDeveSerNegado(event.pedidoId())) {
            PagamentoNegadoEvent pagamentoNegadoEvent = new PagamentoNegadoEvent(
                    event.pedidoId(),
                    event.usuarioId(),
                    event.eventoId(),
                    event.eventoNome(),
                    event.dataEvento(),
                    event.horaEvento(),
                    event.valorTotal(),
                    event.itens()
            );
            logger.info("Pagamento negado para pedidoId={}. Publicando {} no tópico {}",
                    event.pedidoId(), "PAGAMENTO_NEGADO", pagamentoNegadoTopic);
            kafkaTemplate.send(pagamentoNegadoTopic, String.valueOf(event.pedidoId()), pagamentoNegadoEvent);
            return;
        }

        PagamentoConfirmadoEvent pagamentoConfirmadoEvent = new PagamentoConfirmadoEvent(
                event.pedidoId(),
                event.usuarioId(),
                event.eventoId(),
                event.eventoNome(),
                event.dataEvento(),
                event.horaEvento(),
                event.valorTotal(),
                event.itens()
        );
        logger.info("Pagamento confirmado para pedidoId={}. Publicando {} no tópico {}",
                event.pedidoId(), "PAGAMENTO_CONFIRMADO", pagamentoConfirmadoTopic);
        kafkaTemplate.send(pagamentoConfirmadoTopic, String.valueOf(event.pedidoId()), pagamentoConfirmadoEvent);
    }

    boolean pagamentoDeveSerNegado(Long pedidoId) {
        return Math.floorMod(pedidoId, 10) == 6 || Math.floorMod(pedidoId, 10) == 9;
    }

    private void validarPedidoRealizado(PedidoRealizadoEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Evento PEDIDO_REALIZADO é obrigatório");
        }
        if (event.pedidoId() == null || event.pedidoId() <= 0) {
            throw new IllegalArgumentException("Identificador do pedido é obrigatório");
        }
        if (event.usuarioId() == null || event.usuarioId().isBlank()) {
            throw new IllegalArgumentException("Usuário do pedido é obrigatório");
        }
        if (event.eventoId() == null || event.eventoId() <= 0) {
            throw new IllegalArgumentException("Identificador do evento é obrigatório");
        }
        if (event.eventoNome() == null || event.eventoNome().isBlank()) {
            throw new IllegalArgumentException("Nome do evento é obrigatório");
        }
        if (event.dataEvento() == null) {
            throw new IllegalArgumentException("Data do evento é obrigatória");
        }
        if (event.horaEvento() == null) {
            throw new IllegalArgumentException("Hora do evento é obrigatória");
        }
        if (event.valorTotal() == null || event.valorTotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor total do pedido deve ser maior que zero");
        }
        if (event.itens() == null || event.itens().isEmpty()) {
            throw new IllegalArgumentException("Pedido realizado deve conter ao menos um item");
        }
        for (PedidoRealizadoItemEvent item : event.itens()) {
            validarItem(item, event.dataEvento(), event.horaEvento());
        }
    }

    private void validarItem(PedidoRealizadoItemEvent item, LocalDate dataEvento, LocalTime horaEvento) {
        if (item == null) {
            throw new IllegalArgumentException("Itens do pedido devem ser válidos");
        }
        if (item.nomePortador() == null || item.nomePortador().isBlank()) {
            throw new IllegalArgumentException("Nome do portador é obrigatório");
        }
        if (item.cpfPortador() == null || item.cpfPortador().isBlank()) {
            throw new IllegalArgumentException("CPF do portador é obrigatório");
        }
        if (item.tipoIngresso() == null || item.tipoIngresso().isBlank()) {
            throw new IllegalArgumentException("Tipo do ingresso é obrigatório");
        }
        if (item.precoPago() == null || item.precoPago().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Preço pago do ingresso deve ser maior que zero");
        }
        if (dataEvento == null || horaEvento == null) {
            throw new IllegalArgumentException("Dados do evento são obrigatórios");
        }
    }

    public String getPagamentoConfirmadoTopic() {
        return pagamentoConfirmadoTopic;
    }

    public String getPagamentoNegadoTopic() {
        return pagamentoNegadoTopic;
    }
}


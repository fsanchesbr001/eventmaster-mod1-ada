package com.fabriciosanches.orderservice.service;

import com.fabriciosanches.shared.events.PagamentoConfirmadoEvent;
import com.fabriciosanches.shared.events.PagamentoNegadoEvent;
import com.fabriciosanches.shared.events.PedidoCanceladoEvent;
import com.fabriciosanches.shared.events.PedidoConfirmadoEvent;
import com.fabriciosanches.shared.events.PedidoRealizadoEvent;
import com.fabriciosanches.shared.events.PedidoRealizadoItemEvent;
import com.fabriciosanches.orderservice.domain.Order;
import com.fabriciosanches.orderservice.domain.OrderItem;
import com.fabriciosanches.orderservice.dtos.EventResponseDTO;
import com.fabriciosanches.orderservice.dtos.OrderItemRequestDTO;
import com.fabriciosanches.orderservice.dtos.OrderRequestDTO;
import com.fabriciosanches.orderservice.dtos.OrderResponseDTO;
import com.fabriciosanches.orderservice.dtos.ReservaRequestDTO;
import com.fabriciosanches.orderservice.enums.FormaPagamento;
import com.fabriciosanches.orderservice.enums.StatusPedido;
import com.fabriciosanches.orderservice.enums.TipoIngresso;
import com.fabriciosanches.orderservice.exceptions.OrderIntegrationException;
import com.fabriciosanches.orderservice.exceptions.OrderNotFoundException;
import com.fabriciosanches.orderservice.exceptions.OrderValidationException;
import com.fabriciosanches.orderservice.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RestTemplate restTemplate;
    private final String eventServiceUrl;
    private final String ticketServiceUrl;
    private final String pedidoRealizadoTopic;
    private final String pedidoConfirmadoTopic;
    private final String pedidoCanceladoTopic;
    private final boolean pedidoRealizadoPublishingEnabled;

    public OrderService(OrderRepository orderRepository,
                        KafkaTemplate<String, Object> kafkaTemplate,
                        RestTemplate restTemplate,
                        @Value("${endpoint.event-service}") String eventServiceUrl,
                        @Value("${endpoint.ticket-service}") String ticketServiceUrl,
                        @Value("${app.kafka.topic.pedido-realizado}") String pedidoRealizadoTopic,
                        @Value("${app.kafka.topic.pedido-confirmado}") String pedidoConfirmadoTopic,
                        @Value("${app.kafka.topic.pedido-cancelado}") String pedidoCanceladoTopic,
                        @Value("${app.messaging.pedido-realizado.enabled:true}") boolean pedidoRealizadoPublishingEnabled) {
        this.orderRepository = orderRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.restTemplate = restTemplate;
        this.eventServiceUrl = eventServiceUrl;
        this.ticketServiceUrl = ticketServiceUrl;
        this.pedidoRealizadoTopic = pedidoRealizadoTopic;
        this.pedidoConfirmadoTopic = pedidoConfirmadoTopic;
        this.pedidoCanceladoTopic = pedidoCanceladoTopic;
        this.pedidoRealizadoPublishingEnabled = pedidoRealizadoPublishingEnabled;
    }

    @Transactional
    public OrderResponseDTO criarPedido(OrderRequestDTO dto, String usuarioId, String authorizationHeader) {
        validarPedido(dto, usuarioId, authorizationHeader);

        EventResponseDTO evento = buscarEvento(dto.eventId(), authorizationHeader);
        reservarIngressos(dto.eventId(), dto.itens().size(), authorizationHeader);

        Order order = new Order();
        order.setUsuarioId(usuarioId);
        order.setEventoId(dto.eventId());
        order.setFormaPagamento(parseFormaPagamento(dto.formaPagamento()));
        order.setStatus(StatusPedido.REALIZADO);
        order.setDataCriacao(LocalDateTime.now());

        BigDecimal valorTotalPedido = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        for (OrderItemRequestDTO itemDto : dto.itens()) {
            TipoIngresso tipoIngresso = parseTipoIngresso(itemDto.tipoIngresso());
            BigDecimal precoItem = calcularPrecoItem(evento.precoBase(), tipoIngresso);

            OrderItem item = new OrderItem();
            item.setNomePortador(itemDto.nomePortador().trim());
            item.setCpfPortador(itemDto.cpfPortador().trim());
            item.setTipoIngresso(tipoIngresso);
            item.setPrecoPago(precoItem);
            order.adicionarItem(item);

            valorTotalPedido = valorTotalPedido.add(precoItem);
        }

        order.setValorTotal(valorTotalPedido);

        Order savedOrder = orderRepository.save(order);
        publicarPedidoRealizado(savedOrder, evento);

        return new OrderResponseDTO(savedOrder.getId(), savedOrder.getValorTotal(), savedOrder.getStatus().name());
    }

    @Transactional
    public void processarPagamentoConfirmado(PagamentoConfirmadoEvent event) {
        validarPagamentoConfirmado(event);

        Order order = obterPedidoPorId(event.pedidoId());
        validarConsistenciaComPedido(order, event.usuarioId(), event.eventoId(), event.valorTotal());

        if (order.getStatus() == StatusPedido.CONFIRMADO) {
            logger.info("Pedido id={} já está CONFIRMADO. Ignorando reprocessamento do pagamento confirmado.", order.getId());
            return;
        }
        if (order.getStatus() == StatusPedido.CANCELADO) {
            logger.warn("Pedido id={} já está CANCELADO. Evento de pagamento confirmado será ignorado.", order.getId());
            return;
        }

        order.marcarComoConfirmado();
        orderRepository.save(order);
        publicarPedidoConfirmado(order, event);
    }

    @Transactional
    public void processarPagamentoNegado(PagamentoNegadoEvent event) {
        validarPagamentoNegado(event);

        Order order = obterPedidoPorId(event.pedidoId());
        validarConsistenciaComPedido(order, event.usuarioId(), event.eventoId(), event.valorTotal());

        if (order.getStatus() == StatusPedido.CANCELADO) {
            logger.info("Pedido id={} já está CANCELADO. Ignorando reprocessamento do pagamento negado.", order.getId());
            return;
        }
        if (order.getStatus() == StatusPedido.CONFIRMADO) {
            logger.warn("Pedido id={} já está CONFIRMADO. Evento de pagamento negado será ignorado.", order.getId());
            return;
        }

        order.marcarComoCancelado();
        orderRepository.save(order);
        publicarPedidoCancelado(order, event);
    }

    private EventResponseDTO buscarEvento(Long eventId, String authorizationHeader) {
        try {
            ResponseEntity<EventResponseDTO> response = restTemplate.exchange(
                    eventServiceUrl + "/" + eventId,
                    HttpMethod.GET,
                    new HttpEntity<>(buildHeaders(authorizationHeader)),
                    EventResponseDTO.class
            );

            EventResponseDTO evento = response.getBody();
            if (evento == null || evento.id() == null || evento.precoBase() == null) {
                throw new OrderIntegrationException("Evento não encontrado ou sem preço base configurado.");
            }
            return evento;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new OrderNotFoundException("Evento não encontrado para o id informado.");
            }
            throw new OrderIntegrationException("Falha ao consultar o event-service para o evento informado.");
        } catch (RestClientException ex) {
            throw new OrderIntegrationException("Falha ao consultar o event-service para o evento informado.");
        }
    }

    private void reservarIngressos(Long eventId, int quantidade, String authorizationHeader) {
        try {
            ReservaRequestDTO reservaRequest = new ReservaRequestDTO(eventId, quantidade);
            restTemplate.exchange(
                    ticketServiceUrl + "/reserva",
                    HttpMethod.POST,
                    new HttpEntity<>(reservaRequest, buildHeaders(authorizationHeader)),
                    Void.class
            );
        } catch (RestClientException ex) {
            throw new OrderIntegrationException("Ingressos indisponíveis ou esgotados no Redis para o evento informado.");
        }
    }

    private BigDecimal calcularPrecoItem(BigDecimal precoBase, TipoIngresso tipoIngresso) {
        if (precoBase == null || precoBase.compareTo(BigDecimal.ZERO) < 0) {
            throw new OrderIntegrationException("Preço base do evento está inválido.");
        }
        BigDecimal precoNormalizado = precoBase.setScale(2, RoundingMode.HALF_UP);
        if (tipoIngresso == TipoIngresso.MEIA) {
            return precoNormalizado.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        }
        return precoNormalizado;
    }

    private void validarPedido(OrderRequestDTO dto, String usuarioId, String authorizationHeader) {
        if (usuarioId == null || usuarioId.isBlank()) {
            throw new OrderValidationException("Usuário autenticado é obrigatório para criar um pedido");
        }
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new OrderValidationException("Header Authorization é obrigatório para orquestração do pedido");
        }
        if (dto == null) {
            throw new OrderValidationException("Payload do pedido é obrigatório");
        }
        if (dto.eventId() == null || dto.eventId() <= 0) {
            throw new OrderValidationException("Identificador do evento é obrigatório e deve ser maior que zero");
        }
        if (dto.formaPagamento() == null || dto.formaPagamento().isBlank()) {
            throw new OrderValidationException("Forma de pagamento é obrigatória");
        }
        parseFormaPagamento(dto.formaPagamento());
        if (dto.itens() == null || dto.itens().isEmpty()) {
            throw new OrderValidationException("O pedido deve conter pelo menos um item");
        }
        for (OrderItemRequestDTO item : dto.itens()) {
            if (item == null) {
                throw new OrderValidationException("Todos os itens do pedido devem ser válidos");
            }
            if (item.nomePortador() == null || item.nomePortador().isBlank()) {
                throw new OrderValidationException("Nome do portador é obrigatório");
            }
            if (item.cpfPortador() == null || item.cpfPortador().isBlank()) {
                throw new OrderValidationException("CPF do portador é obrigatório");
            }
            if (item.tipoIngresso() == null || item.tipoIngresso().isBlank()) {
                throw new OrderValidationException("Tipo do ingresso é obrigatório");
            }
            parseTipoIngresso(item.tipoIngresso());
        }
    }

    private FormaPagamento parseFormaPagamento(String formaPagamento) {
        try {
            return FormaPagamento.valueOf(formaPagamento.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new OrderValidationException("Forma de pagamento deve ser PIX, CARTAO ou BOLETO");
        }
    }

    private TipoIngresso parseTipoIngresso(String tipoIngresso) {
        try {
            return TipoIngresso.valueOf(tipoIngresso.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new OrderValidationException("Tipo do ingresso deve ser INTEIRA ou MEIA");
        }
    }

    private HttpHeaders buildHeaders(String authorizationHeader) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private Order obterPedidoPorId(Long pedidoId) {
        return orderRepository.findById(pedidoId)
                .orElseThrow(() -> new OrderIntegrationException("Pedido não encontrado para o id informado: " + pedidoId));
    }

    private void validarConsistenciaComPedido(Order order, String usuarioId, Long eventoId, BigDecimal valorTotal) {
        if (!order.getUsuarioId().equals(usuarioId)) {
            throw new OrderIntegrationException("Evento de pagamento inválido para o usuário do pedido informado.");
        }
        if (!order.getEventoId().equals(eventoId)) {
            throw new OrderIntegrationException("Evento de pagamento inválido para o evento do pedido informado.");
        }
        if (order.getValorTotal().compareTo(valorTotal) != 0) {
            throw new OrderIntegrationException("Evento de pagamento inválido para o valor total do pedido informado.");
        }
    }

    private void publicarPedidoRealizado(Order order, EventResponseDTO evento) {
        if (!pedidoRealizadoPublishingEnabled) {
            logger.debug("Publicação do PEDIDO_REALIZADO desabilitada para o pedido id={}", order.getId());
            return;
        }

        PedidoRealizadoEvent event = new PedidoRealizadoEvent(
                order.getId(),
                order.getUsuarioId(),
                order.getEventoId(),
                evento.nome(),
                evento.data(),
                evento.hora(),
                order.getValorTotal(),
                order.getItens().stream()
                        .map(item -> new PedidoRealizadoItemEvent(
                                item.getNomePortador(),
                                item.getCpfPortador(),
                                item.getTipoIngresso().name(),
                                item.getPrecoPago()
                        ))
                        .toList()
        );

        publicarEvento(pedidoRealizadoTopic, String.valueOf(order.getId()), event, "PEDIDO_REALIZADO", order.getId());
    }

    private void publicarPedidoConfirmado(Order order, PagamentoConfirmadoEvent pagamentoConfirmadoEvent) {
        PedidoConfirmadoEvent event = new PedidoConfirmadoEvent(
                pagamentoConfirmadoEvent.pedidoId(),
                pagamentoConfirmadoEvent.usuarioId(),
                pagamentoConfirmadoEvent.eventoId(),
                pagamentoConfirmadoEvent.eventoNome(),
                pagamentoConfirmadoEvent.dataEvento(),
                pagamentoConfirmadoEvent.horaEvento(),
                pagamentoConfirmadoEvent.valorTotal(),
                pagamentoConfirmadoEvent.itens()
        );

        publicarEvento(pedidoConfirmadoTopic, String.valueOf(order.getId()), event, "PEDIDO_CONFIRMADO", order.getId());
    }

    private void publicarPedidoCancelado(Order order, PagamentoNegadoEvent pagamentoNegadoEvent) {
        PedidoCanceladoEvent event = new PedidoCanceladoEvent(
                pagamentoNegadoEvent.pedidoId(),
                pagamentoNegadoEvent.usuarioId(),
                pagamentoNegadoEvent.eventoId(),
                pagamentoNegadoEvent.eventoNome(),
                pagamentoNegadoEvent.dataEvento(),
                pagamentoNegadoEvent.horaEvento(),
                pagamentoNegadoEvent.valorTotal(),
                pagamentoNegadoEvent.itens()
        );

        publicarEvento(pedidoCanceladoTopic, String.valueOf(order.getId()), event, "PEDIDO_CANCELADO", order.getId());
    }

    private void publicarEvento(String topic, String key, Object event, String nomeEvento, Long pedidoId) {
        Runnable publishAction = () -> {
            logger.info("Publicando evento {} no tópico {} para o pedido id={}", nomeEvento, topic, pedidoId);
            kafkaTemplate.send(topic, key, event);
        };

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishAction.run();
                }
            });
            return;
        }

        publishAction.run();
    }

    private void validarPagamentoConfirmado(PagamentoConfirmadoEvent event) {
        validarEventoPagamento(
                event == null ? null : event.pedidoId(),
                event == null ? null : event.usuarioId(),
                event == null ? null : event.eventoId(),
                event == null ? null : event.eventoNome(),
                event == null ? null : event.dataEvento(),
                event == null ? null : event.horaEvento(),
                event == null ? null : event.valorTotal(),
                event == null ? null : event.itens(),
                "PAGAMENTO_CONFIRMADO"
        );
    }

    private void validarPagamentoNegado(PagamentoNegadoEvent event) {
        validarEventoPagamento(
                event == null ? null : event.pedidoId(),
                event == null ? null : event.usuarioId(),
                event == null ? null : event.eventoId(),
                event == null ? null : event.eventoNome(),
                event == null ? null : event.dataEvento(),
                event == null ? null : event.horaEvento(),
                event == null ? null : event.valorTotal(),
                event == null ? null : event.itens(),
                "PAGAMENTO_NEGADO"
        );
    }

    private void validarEventoPagamento(Long pedidoId,
                                        String usuarioId,
                                        Long eventoId,
                                        String eventoNome,
                                        java.time.LocalDate dataEvento,
                                        java.time.LocalTime horaEvento,
                                        BigDecimal valorTotal,
                                        List<PedidoRealizadoItemEvent> itens,
                                        String nomeEvento) {
        if (pedidoId == null || pedidoId <= 0) {
            throw new OrderValidationException("Evento " + nomeEvento + " precisa informar um pedido válido");
        }
        if (usuarioId == null || usuarioId.isBlank()) {
            throw new OrderValidationException("Evento " + nomeEvento + " precisa informar o usuário do pedido");
        }
        if (eventoId == null || eventoId <= 0) {
            throw new OrderValidationException("Evento " + nomeEvento + " precisa informar um evento válido");
        }
        if (eventoNome == null || eventoNome.isBlank()) {
            throw new OrderValidationException("Evento " + nomeEvento + " precisa informar o nome do evento");
        }
        if (dataEvento == null) {
            throw new OrderValidationException("Evento " + nomeEvento + " precisa informar a data do evento");
        }
        if (horaEvento == null) {
            throw new OrderValidationException("Evento " + nomeEvento + " precisa informar a hora do evento");
        }
        if (valorTotal == null || valorTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderValidationException("Evento " + nomeEvento + " precisa informar o valor total do pedido");
        }
        if (itens == null || itens.isEmpty()) {
            throw new OrderValidationException("Evento " + nomeEvento + " precisa informar ao menos um item do pedido");
        }
    }
}


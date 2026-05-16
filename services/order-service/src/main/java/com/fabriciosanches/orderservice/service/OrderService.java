package com.fabriciosanches.orderservice.service;

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
    private final KafkaTemplate<String, PedidoRealizadoEvent> kafkaTemplate;
    private final RestTemplate restTemplate;
    private final String eventServiceUrl;
    private final String ticketServiceUrl;
    private final String pedidoRealizadoTopic;
    private final boolean pedidoRealizadoPublishingEnabled;

    public OrderService(OrderRepository orderRepository,
                        KafkaTemplate<String, PedidoRealizadoEvent> kafkaTemplate,
                        RestTemplate restTemplate,
                        @Value("${endpoint.event-service}") String eventServiceUrl,
                        @Value("${endpoint.ticket-service}") String ticketServiceUrl,
                        @Value("${app.kafka.topic.pedido-realizado}") String pedidoRealizadoTopic,
                        @Value("${app.messaging.pedido-realizado.enabled:true}") boolean pedidoRealizadoPublishingEnabled) {
        this.orderRepository = orderRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.restTemplate = restTemplate;
        this.eventServiceUrl = eventServiceUrl;
        this.ticketServiceUrl = ticketServiceUrl;
        this.pedidoRealizadoTopic = pedidoRealizadoTopic;
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

        Runnable publishAction = () -> {
            logger.info("Publicando evento PEDIDO_REALIZADO no tópico {} para o pedido id={}", pedidoRealizadoTopic, order.getId());
            kafkaTemplate.send(pedidoRealizadoTopic, String.valueOf(order.getId()), event);
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
}


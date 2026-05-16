package com.fabriciosanches.orderservice.service;

import com.fabriciosanches.shared.events.PedidoRealizadoEvent;
import com.fabriciosanches.orderservice.domain.Order;
import com.fabriciosanches.orderservice.dtos.EventResponseDTO;
import com.fabriciosanches.orderservice.dtos.OrderItemRequestDTO;
import com.fabriciosanches.orderservice.dtos.OrderRequestDTO;
import com.fabriciosanches.orderservice.exceptions.OrderIntegrationException;
import com.fabriciosanches.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTests {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private KafkaTemplate<String, PedidoRealizadoEvent> kafkaTemplate;

    @Mock
    private RestTemplate restTemplate;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                orderRepository,
                kafkaTemplate,
                restTemplate,
                "http://localhost:8082/event-service/eventos",
                "http://localhost:8083/ticket-service/ingressos",
                "PEDIDO_REALIZADO",
                true
        );
    }

    @Test
    void shouldCreateOrderReserveStockAndPublishPedidoRealizado() {
        OrderRequestDTO request = new OrderRequestDTO(
                1L,
                "PIX",
                List.of(
                        new OrderItemRequestDTO("Fabricio Sanches", "123.456.789-00", "INTEIRA"),
                        new OrderItemRequestDTO("Ada Lovelace", "987.654.321-00", "MEIA")
                )
        );

        when(restTemplate.exchange(
                eq("http://localhost:8082/event-service/eventos/1"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(EventResponseDTO.class)
        )).thenReturn(ResponseEntity.ok(new EventResponseDTO(
                1L,
                "Show da ADA",
                LocalDate.of(2026, 12, 10),
                LocalTime.of(20, 0, 0),
                new BigDecimal("100.00")
        )));

        when(restTemplate.exchange(
                eq("http://localhost:8083/ticket-service/ingressos/reserva"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Void.class)
        )).thenReturn(ResponseEntity.noContent().build());

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        var response = orderService.criarPedido(request, "usuario-1", "Bearer jwt-teste");

        assertEquals(1L, response.id());
        assertEquals(new BigDecimal("150.00"), response.valorTotal());
        assertEquals("REALIZADO", response.status());

        verify(orderRepository).save(argThat(matchesOrder()));
        verify(kafkaTemplate).send(eq("PEDIDO_REALIZADO"), eq("1"), argThat(matchesPedidoRealizado()));
    }

    @Test
    void shouldFailWhenTicketReservationFails() {
        OrderRequestDTO request = new OrderRequestDTO(
                1L,
                "PIX",
                List.of(new OrderItemRequestDTO("Fabricio Sanches", "123.456.789-00", "INTEIRA"))
        );

        when(restTemplate.exchange(
                eq("http://localhost:8082/event-service/eventos/1"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(EventResponseDTO.class)
        )).thenReturn(ResponseEntity.ok(new EventResponseDTO(
                1L,
                "Show da ADA",
                LocalDate.of(2026, 12, 10),
                LocalTime.of(20, 0, 0),
                new BigDecimal("100.00")
        )));

        when(restTemplate.exchange(
                eq("http://localhost:8083/ticket-service/ingressos/reserva"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Void.class)
        )).thenThrow(new RestClientException("Sem estoque"));

        assertThrows(OrderIntegrationException.class, () -> orderService.criarPedido(request, "usuario-1", "Bearer jwt-teste"));

        verify(orderRepository, never()).save(any(Order.class));
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    private ArgumentMatcher<Order> matchesOrder() {
        return order -> order != null
                && "usuario-1".equals(order.getUsuarioId())
                && Long.valueOf(1L).equals(order.getEventoId())
                && new BigDecimal("150.00").compareTo(order.getValorTotal()) == 0
                && order.getItens().size() == 2;
    }

    private ArgumentMatcher<PedidoRealizadoEvent> matchesPedidoRealizado() {
        return event -> event != null
                && Long.valueOf(1L).equals(event.pedidoId())
                && "usuario-1".equals(event.usuarioId())
                && Long.valueOf(1L).equals(event.eventoId())
                && "Show da ADA".equals(event.eventoNome())
                && LocalDate.of(2026, 12, 10).equals(event.dataEvento())
                && LocalTime.of(20, 0, 0).equals(event.horaEvento())
                && new BigDecimal("150.00").compareTo(event.valorTotal()) == 0
                && event.itens() != null
                && event.itens().size() == 2
                && "Fabricio Sanches".equals(event.itens().get(0).nomePortador())
                && "INTEIRA".equals(event.itens().get(0).tipoIngresso())
                && new BigDecimal("100.00").compareTo(event.itens().get(0).precoPago()) == 0
                && "Ada Lovelace".equals(event.itens().get(1).nomePortador())
                && "MEIA".equals(event.itens().get(1).tipoIngresso())
                && new BigDecimal("50.00").compareTo(event.itens().get(1).precoPago()) == 0;
    }
}


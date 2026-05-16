package com.fabriciosanches.paymentservice.service;

import com.fabriciosanches.shared.events.PagamentoConfirmadoEvent;
import com.fabriciosanches.shared.events.PagamentoNegadoEvent;
import com.fabriciosanches.shared.events.PedidoRealizadoEvent;
import com.fabriciosanches.shared.events.PedidoRealizadoItemEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentProcessorServiceTests {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private PaymentProcessorService paymentProcessorService;

    @BeforeEach
    void setUp() {
        paymentProcessorService = new PaymentProcessorService(kafkaTemplate, "PAGAMENTO_CONFIRMADO", "PAGAMENTO_NEGADO");
    }

    @Test
    void shouldPublishPagamentoNegadoForOrdersEndingWithSix() {
        PedidoRealizadoEvent event = criarPedidoRealizadoEvent(16L);

        paymentProcessorService.processar(event);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("PAGAMENTO_NEGADO"), eq("16"), captor.capture());
        PagamentoNegadoEvent publicado = (PagamentoNegadoEvent) captor.getValue();
        assertEquals(16L, publicado.pedidoId());
    }

    @Test
    void shouldPublishPagamentoNegadoForOrdersEndingWithNine() {
        PedidoRealizadoEvent event = criarPedidoRealizadoEvent(29L);

        paymentProcessorService.processar(event);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("PAGAMENTO_NEGADO"), eq("29"), captor.capture());
        PagamentoNegadoEvent publicado = (PagamentoNegadoEvent) captor.getValue();
        assertEquals(29L, publicado.pedidoId());
    }

    @Test
    void shouldPublishPagamentoConfirmadoForAnyOtherOrder() {
        PedidoRealizadoEvent event = criarPedidoRealizadoEvent(15L);

        paymentProcessorService.processar(event);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("PAGAMENTO_CONFIRMADO"), eq("15"), captor.capture());
        PagamentoConfirmadoEvent publicado = (PagamentoConfirmadoEvent) captor.getValue();
        assertEquals(15L, publicado.pedidoId());
    }

    private PedidoRealizadoEvent criarPedidoRealizadoEvent(Long pedidoId) {
        return new PedidoRealizadoEvent(
                pedidoId,
                "usuario-1",
                10L,
                "Show da ADA",
                LocalDate.of(2026, 12, 10),
                LocalTime.of(20, 0),
                new BigDecimal("150.00"),
                List.of(
                        new PedidoRealizadoItemEvent("Fabricio Sanches", "123.456.789-00", "INTEIRA", new BigDecimal("100.00")),
                        new PedidoRealizadoItemEvent("Ada Lovelace", "987.654.321-00", "MEIA", new BigDecimal("50.00"))
                )
        );
    }
}


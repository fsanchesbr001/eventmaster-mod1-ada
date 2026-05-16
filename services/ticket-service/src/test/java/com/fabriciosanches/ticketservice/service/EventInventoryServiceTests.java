package com.fabriciosanches.ticketservice.service;

import com.fabriciosanches.ticketservice.exceptions.TicketInventoryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventInventoryServiceTests {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private EventInventoryService eventInventoryService;

    @BeforeEach
    void setUp() {
        eventInventoryService = new EventInventoryService(redisTemplate);
    }

    @Test
    void shouldLoadNinetyFivePercentOfCapacityIntoRedis() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        eventInventoryService.carregarEstoqueInicial(1L, 101);

        verify(valueOperations).set("evento:1:ingressos_disponiveis", "95");
        assertEquals("evento:1:ingressos_disponiveis", eventInventoryService.buildInventoryKey(1L));
    }

    @Test
    void shouldThrowWhenThereIsNotEnoughStock() {
        when(redisTemplate.execute(any(RedisScript.class), eq(List.of("evento:1:ingressos_disponiveis")), eq("2")))
                .thenReturn(-2L);

        assertThrows(TicketInventoryException.class, () -> eventInventoryService.reservarIngressos(1L, 2));
    }

    @Test
    void shouldReturnTicketsToRedisWhenOrderIsCancelled() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("evento:1:ingressos_disponiveis", 2)).thenReturn(97L);

        eventInventoryService.devolverIngressos(1L, 2);

        verify(valueOperations).increment("evento:1:ingressos_disponiveis", 2);
    }
}


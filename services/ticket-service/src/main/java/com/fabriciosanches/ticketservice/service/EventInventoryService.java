package com.fabriciosanches.ticketservice.service;

import com.fabriciosanches.ticketservice.exceptions.TicketInventoryException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventInventoryService {

    private static final double FATOR_SEGURANCA = 0.95d;
    private static final Long REDIS_KEY_NOT_FOUND = -1L;
    private static final Long REDIS_INSUFFICIENT_STOCK = -2L;

    private static final DefaultRedisScript<Long> RESERVA_SCRIPT = new DefaultRedisScript<>(
            "local current = redis.call('GET', KEYS[1]) " +
                    "if not current then return -1 end " +
                    "if tonumber(current) < tonumber(ARGV[1]) then return -2 end " +
                    "return redis.call('DECRBY', KEYS[1], ARGV[1])",
            Long.class
    );

    private final StringRedisTemplate redisTemplate;

    public EventInventoryService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void carregarEstoqueInicial(Long eventId, Integer capacidadeOriginal) {
        if (eventId == null) {
            throw new TicketInventoryException("Identificador do evento é obrigatório para carga de estoque");
        }
        if (capacidadeOriginal == null || capacidadeOriginal < 0) {
            throw new TicketInventoryException("Capacidade do evento é obrigatória para carga de estoque");
        }

        int capacidadeRedis = (int) Math.floor(capacidadeOriginal * FATOR_SEGURANCA);
        redisTemplate.opsForValue().set(buildInventoryKey(eventId), String.valueOf(capacidadeRedis));
    }

    public void reservarIngressos(Long eventId, Integer quantidade) {
        if (eventId == null || eventId <= 0) {
            throw new TicketInventoryException("Identificador do evento é obrigatório para reserva");
        }
        if (quantidade == null || quantidade <= 0) {
            throw new TicketInventoryException("Quantidade de ingressos deve ser maior que zero");
        }

        Long result = redisTemplate.execute(RESERVA_SCRIPT, List.of(buildInventoryKey(eventId)), String.valueOf(quantidade));
        if (result == null) {
            throw new TicketInventoryException("Falha ao reservar ingressos no Redis");
        }
        if (REDIS_KEY_NOT_FOUND.equals(result)) {
            throw new TicketInventoryException("Estoque do evento ainda não foi carregado no Redis");
        }
        if (REDIS_INSUFFICIENT_STOCK.equals(result)) {
            throw new TicketInventoryException("Ingressos indisponíveis ou esgotados para o evento informado");
        }
    }

    public String buildInventoryKey(Long eventId) {
        return "evento:" + eventId + ":ingressos_disponiveis";
    }
}


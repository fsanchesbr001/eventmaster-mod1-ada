package com.fabriciosanches.userservice.security;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serviço responsável por manter a blacklist de tokens JWT invalidados via logout.
 * <p>
 * Como JWT é stateless, o logout é implementado adicionando o token a uma lista negra
 * em memória. O token permanece na lista até atingir sua expiração original, quando é
 * removido automaticamente por uma tarefa agendada.
 * </p>
 */
@Service
public class TokenBlacklistService {

    private static final Logger logger = LogManager.getLogger(TokenBlacklistService.class);

    /**
     * Mapa de tokens invalidados: chave = token JWT, valor = instante de expiração original.
     */
    private final Map<String, Instant> blacklist = new ConcurrentHashMap<>();

    /**
     * Adiciona um token à blacklist até que expire.
     *
     * @param token     token JWT a ser invalidado
     * @param expiresAt instante de expiração original do token
     */
    public void revogar(String token, Instant expiresAt) {
        logger.info("Token adicionado à blacklist. Expira em: {}", expiresAt);
        blacklist.put(token, expiresAt);
    }

    /**
     * Verifica se um token está revogado (presente na blacklist).
     *
     * @param token token JWT a verificar
     * @return {@code true} se o token foi revogado via logout
     */
    public boolean estaRevogado(String token) {
        return blacklist.containsKey(token);
    }

    /**
     * Tarefa agendada que limpa da blacklist os tokens que já expiraram naturalmente,
     * evitando crescimento ilimitado da estrutura em memória.
     * Executada a cada 10 minutos.
     */
    @Scheduled(fixedRateString = "${api.security.token.blacklist-cleanup-ms:600000}")
    public void limparTokensExpirados() {
        Instant agora = Instant.now();
        int antes = blacklist.size();
        blacklist.entrySet().removeIf(entry -> entry.getValue().isBefore(agora));
        int removidos = antes - blacklist.size();
        if (removidos > 0) {
            logger.info("Blacklist: {} token(s) expirado(s) removido(s). Total atual: {}", removidos, blacklist.size());
        }
    }
}


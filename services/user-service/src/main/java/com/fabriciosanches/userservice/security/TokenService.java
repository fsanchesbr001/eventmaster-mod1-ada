package com.fabriciosanches.userservice.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.fabriciosanches.userservice.domain.Usuario;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
public class TokenService {

    @Value("${api.security.token.secret}")
    private String secret;

    @Getter
    @Value("${api.security.token.expiration-minutes:120}")
    private long expirationMinutes;

    @Value("${api.security.token.time-zone:America/Sao_Paulo}")
    private String tokenTimeZone;

    @Value("${api.security.token.issuer:API Event Master}")
    private String issuer;

    private final Logger logger = LogManager.getLogger(TokenService.class);

    public String gerarToken(Usuario usuario){
        logger.info("Inicio do método gerarToken");
        try {
            var algoritimo = Algorithm.HMAC256(secret);
            logger.info("Fim do método gerarToken");
            return JWT.create()
                    .withIssuer(issuer)
                    .withSubject(usuario.getLogin())
                    .withClaim("role", usuario.getRole().getRole())
                    .withClaim("nome", usuario.getNome())
                    .withExpiresAt(dataExpiracao())
                    .sign(algoritimo);

        } catch (JWTCreationException exception){
            throw new RuntimeException("Erro ao gerar Token JWT",exception);
        }
    }

    public String getSubject(String tokenJWT){
        try {
            logger.info("Inicio do método getSubject");
            var algoritimo = Algorithm.HMAC256(secret);
            logger.info("Fim do método getSubject");
            return JWT.require(algoritimo)
                    .withIssuer(issuer)
                    .build()
                    .verify(tokenJWT)
                    .getSubject();
        } catch (JWTVerificationException exception){
            throw new RuntimeException("Verificação de Token falhou!!!",exception);
        }
    }

    public String getRole(String tokenJWT){
        try {
            logger.info("Inicio do método getRole");
            var algoritimo = Algorithm.HMAC256(secret);
            logger.info("Fim do método getRole");
            return JWT.require(algoritimo)
                    .withIssuer(issuer)
                    .build()
                    .verify(tokenJWT)
                    .getClaim("role").asString();
        } catch (JWTVerificationException exception){
            throw new RuntimeException("Verificação de Token falhou!!!",exception);
        }
    }

    public OffsetDateTime getTokenExpiresAt() {
        return dataExpiracao().atZone(getTokenZoneId()).toOffsetDateTime();
    }

    private Instant dataExpiracao() {
        return ZonedDateTime.now(getTokenZoneId())
                .plusMinutes(expirationMinutes)
                .toInstant();
    }

    private ZoneId getTokenZoneId() {
        return ZoneId.of(tokenTimeZone);
    }


    public Instant getExpiration(String tokenJWT) {
        try {
            var algoritimo = Algorithm.HMAC256(secret);
            var decoded = JWT.require(algoritimo)
                    .withIssuer(issuer)
                    .build()
                    .verify(tokenJWT);
            return decoded.getExpiresAtAsInstant();
        } catch (JWTVerificationException exception) {
            logger.warn("Não foi possível extrair expiração do token: {}", exception.getMessage());
            return null;
        }
    }

    public boolean validarTokenExpirado(String tokenJWT) {
        try {
            logger.info("Validando expiração do token");
            var algoritimo = Algorithm.HMAC256(secret);
            JWT.require(algoritimo)
                    .withIssuer(issuer)
                    .build()
                    .verify(tokenJWT);
            logger.info("Token válido e não expirado");
            return true;
        } catch (TokenExpiredException exception) {
            logger.warn("Token expirado: {}", exception.getMessage());
            return false;
        } catch (JWTVerificationException exception) {
            logger.warn("Erro ao verificar token: {}", exception.getMessage());
            return false;
        }
    }
}

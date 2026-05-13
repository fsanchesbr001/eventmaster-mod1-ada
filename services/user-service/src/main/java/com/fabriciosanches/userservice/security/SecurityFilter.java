package com.fabriciosanches.userservice.security;

import com.fabriciosanches.userservice.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    private static final Logger log = LogManager.getLogger(SecurityFilter.class);

    private final TokenService tokenService;
    private final UsuarioRepository repository;
    private final TokenBlacklistService tokenBlacklistService;

    public SecurityFilter(TokenService tokenService, UsuarioRepository repository,
                          TokenBlacklistService tokenBlacklistService) {
        this.tokenService = tokenService;
        this.repository = repository;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        // Preflight OPTIONS: deixar passar sem validar token para que o CORS funcione
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String uri = request.getMethod() + " " + request.getRequestURI();
        var tokenJWT = recuperarToken(request);

        if (tokenJWT == null) {
            log.warn("[SecurityFilter] {} - Nenhum token encontrado no header Authorization. Requisição anônima.", uri);
            filterChain.doFilter(request, response);
            return;
        }

        // Validar se o token está expirado
        if (!tokenService.validarTokenExpirado(tokenJWT)) {
            log.warn("[SecurityFilter] {} - Token expirado.", uri);
            writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Token expirado", "Seu token de autenticação expirou. Por favor, faça login novamente.");
            return;
        }

        // Validar se o token foi revogado via logout
        if (tokenBlacklistService.estaRevogado(tokenJWT)) {
            log.warn("[SecurityFilter] {} - Token revogado.", uri);
            writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Token revogado", "Sua sessão foi encerrada. Por favor, faça login novamente.");
            return;
        }

        try {
            var subject = tokenService.getSubject(tokenJWT);
            var role    = tokenService.getRole(tokenJWT);

            log.info("[SecurityFilter] {} - subject='{}' | role extraída do token='{}'", uri, subject, role);

            if (role == null || role.isBlank()) {
                log.error("[SecurityFilter] {} - Claim 'role' ausente ou vazia no token do usuário '{}'", uri, subject);
                writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "Token inválido", "O token não contém uma role válida. Faça login novamente.");
                return;
            }

            if (!role.startsWith("ROLE_")) {
                role = "ROLE_" + role;
            }

            var authority      = new SimpleGrantedAuthority(role);
            var usuario        = repository.findByLogin(subject);
            var authentication = new UsernamePasswordAuthenticationToken(usuario, tokenJWT,
                    Collections.singletonList(authority));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.info("[SecurityFilter] {} - Autenticação definida | authority='{}'", uri, role);

        } catch (Exception e) {
            log.error("[SecurityFilter] {} - Erro ao processar token: {}", uri, e.getMessage(), e);
            writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Token inválido", "Seu token de autenticação é inválido. Por favor, faça login novamente.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeJsonError(HttpServletResponse response, int status, String error, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        Map<String, String> errorMap = new HashMap<>();
        errorMap.put("error", error);
        errorMap.put("message", message);
        response.getWriter().write(new ObjectMapper().writeValueAsString(errorMap));
    }

    private String recuperarToken(HttpServletRequest request) {
        var authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }
        var value = authorizationHeader.trim();
        if (value.regionMatches(true, 0, "Bearer", 0, "Bearer".length())) {
            value = value.substring("Bearer".length()).trim();
        }
        return value.isBlank() ? null : value;
    }
}

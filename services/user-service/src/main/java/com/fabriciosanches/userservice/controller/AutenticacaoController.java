package com.fabriciosanches.userservice.controller;

import com.fabriciosanches.userservice.domain.Usuario;
import com.fabriciosanches.userservice.dtos.AutenticacaoDTO;
import com.fabriciosanches.userservice.exceptions.UsuarioException;
import com.fabriciosanches.userservice.repository.UsuarioRepository;
import com.fabriciosanches.userservice.security.DadosTokenJWT;
import com.fabriciosanches.userservice.security.TokenBlacklistService;
import com.fabriciosanches.userservice.security.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticacao", description = "Operacoes de login e logout para emissao e revogacao de JWT")
public class AutenticacaoController {

    private final AuthenticationConfiguration authenticationConfiguration;
    private final TokenService tokenService;
    private final TokenBlacklistService tokenBlacklistService;

    public AutenticacaoController(AuthenticationConfiguration authenticationConfiguration,
                                  TokenService tokenService,
                                  UsuarioRepository usuarioRepository,
                                  TokenBlacklistService tokenBlacklistService) {
        this.authenticationConfiguration = authenticationConfiguration;
        this.tokenService = tokenService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    //Testado
    @PostMapping({"/login", "/login/"})
    @Operation(summary = "Efetuar login", description = "Autentica usuario e retorna JWT para uso nos endpoints protegidos.")
    @SecurityRequirements
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Credenciais de login",
            content = @Content(
                    schema = @Schema(implementation = com.fabriciosanches.userservice.dtos.AutenticacaoDTO.class),
                    examples = @ExampleObject(name = "loginRequest", value = "{\"login\":\"admin@eventmaster.com\",\"senha\":\"Senha@123\"}")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login realizado com sucesso", content = @Content(
                    schema = @Schema(implementation = DadosTokenJWT.class),
                    examples = @ExampleObject(name = "tokenResponse", value = "{\"jwt\":\"eyJhbGciOiJIUzI1NiJ9...\",\"expirationMinutes\":120,\"expiresAt\":\"2026-05-14T12:30:00-03:00\",\"usuarioLogin\":\"admin@eventmaster.com\",\"usuarioNome\":\"Administrador\",\"role\":\"ROLE_ADMIN\"}"))),
            @ApiResponse(responseCode = "400", description = "Credenciais invalidas", content = @Content(
                    schema = @Schema(implementation = DadosTokenJWT.class),
                    examples = @ExampleObject(name = "loginErro", value = "{\"jwt\":\"PROBLEMAS DE AUTENTICACAO, CONTATE O ADM\"}"))),
            @ApiResponse(responseCode = "500", description = "Erro interno na autenticacao", content = @Content(
                    schema = @Schema(implementation = DadosTokenJWT.class),
                    examples = @ExampleObject(name = "loginErroInterno", value = "{\"jwt\":\"Erro interno\"}")))
    })
    public ResponseEntity<DadosTokenJWT> efetuarLogin(@RequestBody @Valid AutenticacaoDTO dados){
        LoggerFactory.getLogger(this.getClass()).info("Fluxo entrou no método efetuarLogin - Usuário: {}", dados.login());

        if(dados.login() == null || dados.senha() == null){
            return ResponseEntity.badRequest().build();
        }
        try{
            var userPwd = new UsernamePasswordAuthenticationToken(dados.login(),dados.senha());
            var authentication = authenticationConfiguration.getAuthenticationManager().authenticate(userPwd);

            var usuario = (Usuario) authentication.getPrincipal();
            var token = tokenService.gerarToken(usuario);
            var expirationMinutes = tokenService.getExpirationMinutes();
            var expiresAt = tokenService.getTokenExpiresAt();
            var role = usuario.getRole().getRole();

            return ResponseEntity.ok(new DadosTokenJWT(
                token,
                expirationMinutes,
                expiresAt,
                usuario.getLogin(),
                usuario.getNome(),
                role
            ));
        }catch (BadCredentialsException e){
            return ResponseEntity.badRequest().body(new DadosTokenJWT(
                    "PROBLEMAS DE AUTENTICAÇÃO, CONTATE O ADM"));
        }
        catch (AuthenticationException e) {
            return ResponseEntity.status(401).body(new DadosTokenJWT(e.getMessage()));
        }
        catch (UsuarioException ex) {
            return ResponseEntity.badRequest().body(new DadosTokenJWT(ex.getMessage()));
        }
        catch (Exception e){
            return ResponseEntity.internalServerError().body(new DadosTokenJWT(e.getMessage()));
        }
    }

    /**
     * Encerra a sessão do usuário invalidando o JWT Token atual.
     * <p>
     * O token é recuperado das credenciais do {@link SecurityContextHolder} (armazenado
     * pelo {@code SecurityFilter}) e adicionado a uma blacklist em memória. Todas as
     * requisições subsequentes com esse token serão rejeitadas com 401.
     * </p>
     *
     * @return 200 OK com mensagem de confirmação, ou 400 se não houver sessão ativa
     */
    @PostMapping({"/logout", "/logout/"})
    @Operation(summary = "Efetuar logout", description = "Revoga o JWT atual e encerra a sessao do usuario autenticado.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logout realizado com sucesso", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(name = "logoutOk", value = "{\"message\":\"Logout realizado com sucesso. Sessao encerrada.\"}"))),
            @ApiResponse(responseCode = "400", description = "Sem sessao ativa ou token invalido", content = @Content(
                    mediaType = "application/json",
                    examples = {
                            @ExampleObject(name = "semSessao", value = "{\"error\":\"Nenhuma sessao ativa encontrada\"}"),
                            @ExampleObject(name = "tokenInvalido", value = "{\"error\":\"Token invalido ou nao foi possivel determinar sua expiracao\"}")
                    })),
            @ApiResponse(responseCode = "401", description = "Token ausente ou invalido")
    })
    public ResponseEntity<Map<String, String>> efetuarLogout() {
        LoggerFactory.getLogger(this.getClass()).info("Fluxo entrou no método efetuarLogout");

        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getCredentials() == null) {
            LoggerFactory.getLogger(this.getClass()).warn("Logout chamado sem sessão ativa no SecurityContext");
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Nenhuma sessão ativa encontrada"));
        }

        String token = (String) auth.getCredentials();

        Instant expiracao = tokenService.getExpiration(token);
        if (expiracao == null) {
            LoggerFactory.getLogger(this.getClass()).warn("Logout: não foi possível determinar a expiração do token");
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Token inválido ou não foi possível determinar sua expiração"));
        }

        tokenBlacklistService.revogar(token, expiracao);
        SecurityContextHolder.clearContext();

        LoggerFactory.getLogger(this.getClass()).info("Logout realizado com sucesso. Token revogado até: {}", expiracao);

        return ResponseEntity.ok(Map.of("message", "Logout realizado com sucesso. Sessão encerrada."));
    }
}

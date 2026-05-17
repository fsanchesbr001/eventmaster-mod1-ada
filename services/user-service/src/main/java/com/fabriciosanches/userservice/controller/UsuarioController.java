package com.fabriciosanches.userservice.controller;

import com.fabriciosanches.userservice.domain.Usuario;
import com.fabriciosanches.userservice.dtos.RegisterDTO;
import com.fabriciosanches.userservice.dtos.RoleOptionDTO;
import com.fabriciosanches.userservice.dtos.UserRolesDTO;
import com.fabriciosanches.userservice.dtos.UsuarioListagemDTO;
import com.fabriciosanches.userservice.enums.UserRole;
import com.fabriciosanches.userservice.exceptions.UsuarioException;
import com.fabriciosanches.userservice.service.SegurancaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("user-service/usuarios")
@Tag(name = "Usuarios", description = "Operacoes de administracao e consulta de usuarios")
public class UsuarioController {

    private static final Logger logger = LogManager.getLogger(UsuarioController.class);

    private final SegurancaService segurancaService;

    public UsuarioController(SegurancaService segurancaService) {
        this.segurancaService = segurancaService;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/roles")
    @Operation(summary = "Listar roles disponiveis", description = "Retorna os perfis de acesso permitidos para cadastro e exibicao no frontend.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Roles retornadas com sucesso", content = @Content(
                    schema = @Schema(implementation = UserRolesDTO.class),
                    examples = @ExampleObject(name = "roles", value = "{\"roles\":[{\"value\":\"ADMIN\",\"label\":\"Administrador\",\"labelKey\":\"role.ADMIN\"},{\"value\":\"SYSTEM\",\"label\":\"Sistema\",\"labelKey\":\"role.SYSTEM\"},{\"value\":\"USER\",\"label\":\"Usuario\",\"labelKey\":\"role.USER\"}]}"))),
            @ApiResponse(responseCode = "400", description = "Falha ao montar lista de roles", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(name = "erroPadrao", value = "{\"error\":\"BAD_REQUEST\",\"message\":\"Erro ao listar roles\"}")))
    })
    public ResponseEntity<?> listarRoles() {
        logger.info("Inicio do método listarRoles - UsuarioController");
        try {
            // Mapeamento default de label em pt-BR; frontend pode usar labelKey para i18n
            Map<UserRole, String> defaultLabels = Map.of(
                    UserRole.ADMIN, "Administrador",
                    UserRole.USER, "Usuário",
                    UserRole.SYSTEM, "Sistema"
            );

            List<RoleOptionDTO> options = Stream.of(UserRole.values())
                    .map(r -> new RoleOptionDTO(r.name(), defaultLabels.getOrDefault(r, r.name()), "role." + r.name()))
                    .sorted((a, b) -> a.label().compareToIgnoreCase(b.label()))
                    .collect(Collectors.toList());

            UserRolesDTO dto = new UserRolesDTO(options);
            logger.info("Fim do método listarRoles - UsuarioController");
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            logger.error("Erro ao listar roles", e);
            return ResponseEntity.badRequest().body(buildError("BAD_REQUEST", "Erro ao listar roles"));
        }
    }

    //Testado
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/registrar-usuario")
    @Transactional
    @Operation(summary = "Registrar usuario", description = "Cria um novo usuario no sistema. Requer role ADMIN.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Dados de cadastro do usuario",
            content = @Content(
                    schema = @Schema(implementation = RegisterDTO.class),
                    examples = @ExampleObject(name = "novoUsuario", value = "{\"login\":\"novo.usuario@eventmaster.com\",\"senha\":\"Senha@123\",\"role\":\"USER\",\"nome\":\"Novo Usuario\",\"cpf\":\"12345678900\"}")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuario registrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos ou regra de negocio violada", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(name = "erroValidacao", value = "{\"error\":\"BAD_REQUEST\",\"message\":\"Login ja cadastrado\"}"))),
            @ApiResponse(responseCode = "500", description = "Erro interno ao registrar usuario", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(name = "erroInterno", value = "{\"error\":\"INTERNAL_SERVER_ERROR\",\"message\":\"Erro interno ao registrar usuario\"}")))
    })
    public ResponseEntity<?> registrarUsuario(@RequestBody RegisterDTO dados) {
        logger.info("Inicio do método registrarUsuario - UsuarioController");
        logger.info("Parâmetros de entrada: {}", dados);
        try {
            segurancaService.registrarUsuario(dados);
            logger.info("Usuário registrado com sucesso");
            logger.info("Fim do método registrarUsuario");
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (UsuarioException e) {
            logger.error("Erro ao registrar usuário", e);
            return ResponseEntity.badRequest().body(buildError("BAD_REQUEST", e.getMessage()));
        } catch (Exception e) {
            logger.error("Erro inesperado ao registrar usuário", e);
            return ResponseEntity.status(500).body(buildError("INTERNAL_SERVER_ERROR", "Erro interno ao registrar usuario"));
        }
    }

    //Testado
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/excluir-usuario")
    @Transactional
    @Operation(summary = "Excluir usuario", description = "Exclui um usuario pelo login informado no payload. Requer role ADMIN.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Dados do usuario a ser removido (campo login obrigatorio)",
            content = @Content(
                    schema = @Schema(implementation = UsuarioListagemDTO.class),
                    examples = @ExampleObject(name = "usuarioParaExclusao", value = "{\"nome\":\"Usuario Alvo\",\"role\":\"USER\",\"login\":\"usuario.alvo@eventmaster.com\"}")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Usuario excluido com sucesso"),
            @ApiResponse(responseCode = "400", description = "Erro de validacao ou usuario inexistente", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(name = "erroNegocio", value = "{\"error\":\"BAD_REQUEST\",\"message\":\"Usuario nao encontrado para exclusao\"}")))
    })
    public ResponseEntity<?> excluirUsuario(@RequestBody UsuarioListagemDTO dados) {
        logger.info("Inicio do método excluirUsuario - UsuarioController");
        logger.info("Parâmetros de entrada: {}", dados);
        try {
            segurancaService.excluirUsuario(dados.login());
            logger.info("Usuário excluído com sucesso");
            logger.info("Fim do método excluirUsuario");
            return ResponseEntity.noContent().build();
        } catch (UsuarioException e) {
            logger.error("Erro ao excluir usuário", e);
            return ResponseEntity.badRequest().body(buildError("BAD_REQUEST", e.getMessage()));
        }
    }



    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/atualizar-usuario/{email}")
    @Transactional
    @Operation(summary = "Atualizar usuario", description = "Atualiza os dados de um usuario. Requer role ADMIN.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Dados atualizados do usuario. O email da rota localiza o registro; o campo login do body pode ser usado para alterar o email.",
            content = @Content(
                    schema = @Schema(implementation = UsuarioListagemDTO.class),
                    examples = @ExampleObject(name = "usuarioAtualizado", value = "{\"nome\":\"Usuario Atualizado\",\"role\":\"SYSTEM\",\"login\":\"usuario.alvo@eventmaster.com\"}")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario atualizado", content = @Content(
                    schema = @Schema(implementation = UsuarioListagemDTO.class),
                    examples = @ExampleObject(name = "respostaAtualizacao", value = "{\"nome\":\"Usuario Atualizado\",\"role\":\"SYSTEM\",\"login\":\"usuario.alvo@eventmaster.com\"}"))),
            @ApiResponse(responseCode = "409", description = "Conflito de login ao atualizar usuario", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(name = "erroConflitoLogin", value = "{\"error\":\"CONFLICT\",\"message\":\"Usuário já existe com o login: usuario.alvo@eventmaster.com\"}"))),
            @ApiResponse(responseCode = "400", description = "Falha ao atualizar usuario", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(name = "erroAtualizacao", value = "{\"error\":\"BAD_REQUEST\",\"message\":\"Nao foi possivel atualizar usuario\"}")))
    })
    public ResponseEntity<?> atualizarUsuario(
            @Parameter(description = "Email/login do usuario alvo", example = "usuario.alvo@eventmaster.com")
            @PathVariable String email,
            @RequestBody UsuarioListagemDTO dados) {
        logger.info("Inicio do método atualizarUsuario - UsuarioController");
        logger.info("Parâmetro de entrada - email: {}", email);
        logger.info("Parâmetros de entrada - body: {}", dados);
        try {
            UsuarioListagemDTO atualizado = segurancaService.atualizarUsuario(email, dados);
            logger.info("Usuário atualizado com sucesso");
            logger.info("Fim do método atualizarUsuario");
            return ResponseEntity.ok(atualizado);
        } catch (UsuarioException e) {
            logger.error("Erro ao atualizar usuário", e);
            if (e.getMessage() != null && e.getMessage().startsWith("Usuário já existe com o login:")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(buildError("CONFLICT", e.getMessage()));
            }
            return ResponseEntity.badRequest().body(buildError("BAD_REQUEST", "Nao foi possivel atualizar usuario"));
        }
    }

    @GetMapping("/listar-todos-usuarios")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar todos os usuarios", description = "Retorna todos os usuarios cadastrados. Requer role ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de usuarios retornada", content = @Content(
                    array = @ArraySchema(schema = @Schema(implementation = Usuario.class)),
                    examples = @ExampleObject(name = "usuarios", value = "[{\"id\":1,\"login\":\"admin@eventmaster.com\",\"role\":\"ADMIN\",\"nome\":\"Administrador\"}]"))),
            @ApiResponse(responseCode = "204", description = "Nenhum usuario encontrado"),
            @ApiResponse(responseCode = "400", description = "Erro ao listar usuarios", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(name = "erroListagem", value = "{\"error\":\"BAD_REQUEST\",\"message\":\"Erro ao listar usuarios\"}")))
    })
    public ResponseEntity<?> listarTodosUsuarios() {
        logger.info("Inicio do método listarTodosUsuarios - UsuarioController");
        try {
            List<Usuario> response = segurancaService.findAll();
            logger.info("Lista de usuários obtida com sucesso");
            if (response.isEmpty()) {
                logger.info("Nenhum usuário encontrado");
                return ResponseEntity.noContent().build();
            }
            logger.info("Fim do método listarTodosUsuarios");
            return ResponseEntity.ok(response);
        } catch (UsuarioException e) {
            logger.error("Erro ao listar usuários", e);
            return ResponseEntity.badRequest().body(buildError("BAD_REQUEST", "Erro ao listar usuarios"));
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM')")
    @GetMapping("/buscar-usuario/{email}")
    @Operation(summary = "Buscar usuario por email", description = "Busca um usuario pelo login/email informado na rota.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario encontrado", content = @Content(
                    schema = @Schema(implementation = UsuarioListagemDTO.class),
                    examples = @ExampleObject(name = "usuarioEncontrado", value = "{\"nome\":\"Administrador\",\"role\":\"ADMIN\",\"login\":\"admin@eventmaster.com\"}"))),
            @ApiResponse(responseCode = "404", description = "Usuario nao encontrado", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(name = "naoEncontrado", value = "{\"error\":\"NOT_FOUND\",\"message\":\"Usuario nao encontrado\"}"))),
            @ApiResponse(responseCode = "400", description = "Erro ao buscar usuario", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(name = "erroBusca", value = "{\"error\":\"BAD_REQUEST\",\"message\":\"Nao foi possivel buscar usuario\"}")))
    })
    public ResponseEntity<?> buscarUsuarioPorEmail(
            @Parameter(description = "Email/login do usuario", example = "admin@eventmaster.com")
            @PathVariable String email) {
        logger.info("Inicio do método buscarUsuarioPorEmail - UsuarioController");
        logger.info("Parâmetro de entrada: {}", email);
        try {
            UsuarioListagemDTO usuarioListagemDTO = segurancaService.findByEmail(email);
            if (usuarioListagemDTO == null) {
                logger.warn("Usuário não encontrado para o email: {}", email);
                return ResponseEntity.status(404).body(buildError("NOT_FOUND", "Usuario nao encontrado"));
            }
            logger.info("Usuário encontrado com sucesso");
            logger.info("Fim do método buscarUsuarioPorEmail");
            return ResponseEntity.ok(usuarioListagemDTO);
        } catch (UsuarioException e) {
            logger.error("Erro ao buscar usuário por email", e);
            return ResponseEntity.badRequest().body(buildError("BAD_REQUEST", "Nao foi possivel buscar usuario"));
        }
    }

    private Map<String, String> buildError(String error, String message) {
        return Map.of("error", error, "message", message);
    }
}

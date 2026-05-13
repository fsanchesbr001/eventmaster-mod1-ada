package com.fabriciosanches.userservice.controller;

import com.fabriciosanches.userservice.domain.Usuario;
import com.fabriciosanches.userservice.dtos.RegisterDTO;
import com.fabriciosanches.userservice.dtos.RoleOptionDTO;
import com.fabriciosanches.userservice.dtos.UserRolesDTO;
import com.fabriciosanches.userservice.dtos.UsuarioListagemDTO;
import com.fabriciosanches.userservice.enums.UserRole;
import com.fabriciosanches.userservice.exceptions.UsuarioException;
import com.fabriciosanches.userservice.service.SegurancaService;
import jakarta.transaction.Transactional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("user-service/usuarios")
public class UsuarioController {

    private static final Logger logger = LogManager.getLogger(UsuarioController.class);

    private final SegurancaService segurancaService;

    public UsuarioController(SegurancaService segurancaService) {
        this.segurancaService = segurancaService;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/roles")
    public ResponseEntity<UserRolesDTO> listarRoles() {
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
            return ResponseEntity.badRequest().build();
        }
    }

    //Testado
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/registrar-usuario")
    @Transactional
    public ResponseEntity<?> registrarUsuario(@RequestBody RegisterDTO dados) {
        logger.info("Inicio do método registrarUsuario - UsuarioController");
        logger.info("Parâmetros de entrada: {}", dados);
        try {
            segurancaService.registrarUsuario(dados);
            logger.info("Usuário registrado com sucesso");
            logger.info("Fim do método registrarUsuario");
            return ResponseEntity.ok().build();
        } catch (UsuarioException e) {
            logger.error("Erro ao registrar usuário", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Erro inesperado ao registrar usuário", e);
            return ResponseEntity.status(500).body("Erro interno ao registrar usuário");
        }
    }

    //Testado
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/excluir-usuario")
    @Transactional
    public ResponseEntity<?> excluirUsuario(@RequestBody UsuarioListagemDTO dados) {
        logger.info("Inicio do método excluirUsuario - UsuarioController");
        logger.info("Parâmetros de entrada: {}", dados);
        try {
            segurancaService.excluirUsuario(dados.login());
            logger.info("Usuário excluído com sucesso");
            logger.info("Fim do método excluirUsuario");
            return ResponseEntity.ok().build();
        } catch (UsuarioException e) {
            logger.error("Erro ao excluir usuário", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }



    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/atualizar-usuario/{email}")
    @Transactional
    public ResponseEntity<UsuarioListagemDTO> atualizarUsuario(@RequestBody UsuarioListagemDTO dados) {
        logger.info("Inicio do método atualizarUsuario - UsuarioController");
        logger.info("Parâmetro de entrada - email: {}", dados.login());
        logger.info("Parâmetros de entrada - body: {}", dados);
        try {
            UsuarioListagemDTO atualizado = segurancaService.atualizarUsuario(dados);
            logger.info("Usuário atualizado com sucesso");
            logger.info("Fim do método atualizarUsuario");
            return ResponseEntity.ok(atualizado);
        } catch (UsuarioException e) {
            logger.error("Erro ao atualizar usuário", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/listar-todos-usuarios")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Usuario>> listarTodosUsuarios() {
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
            return ResponseEntity.badRequest().build();
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM')")
    @GetMapping("/buscar-usuario/{email}")
    public ResponseEntity<UsuarioListagemDTO> buscarUsuarioPorEmail(@PathVariable String email) {
        logger.info("Inicio do método buscarUsuarioPorEmail - UsuarioController");
        logger.info("Parâmetro de entrada: {}", email);
        try {
            UsuarioListagemDTO usuarioListagemDTO = segurancaService.findByEmail(email);
            if (usuarioListagemDTO == null) {
                logger.warn("Usuário não encontrado para o email: {}", email);
                return ResponseEntity.notFound().build();
            }
            logger.info("Usuário encontrado com sucesso");
            logger.info("Fim do método buscarUsuarioPorEmail");
            return ResponseEntity.ok(usuarioListagemDTO);
        } catch (UsuarioException e) {
            logger.error("Erro ao buscar usuário por email", e);
            return ResponseEntity.badRequest().build();
        }
    }
}

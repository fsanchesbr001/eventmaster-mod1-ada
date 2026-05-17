package com.fabriciosanches.userservice.service;

import com.fabriciosanches.userservice.domain.Usuario;
import com.fabriciosanches.userservice.dtos.*;
import com.fabriciosanches.userservice.enums.UserRole;
import com.fabriciosanches.userservice.exceptions.UsuarioException;
import com.fabriciosanches.userservice.repository.UsuarioRepository;
import com.fabriciosanches.userservice.util.Utilidades;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SegurancaService {

    private final Logger logger = LogManager.getLogger(SegurancaService.class);
    private final UsuarioRepository usuarioRepository;

    public SegurancaService(  UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public void registrarUsuario(RegisterDTO registerDTO)  {
        logger.info("Iniciando processo de registro de usuário");
        if (usuarioRepository.findByLogin(registerDTO.login()) != null) {
            logger.warn("Usuário já existe com o login: {}", registerDTO.login());
            throw new UsuarioException("Usuário já existe com o login: " + registerDTO.login());
        }
        var senhaGerada = Utilidades.encriptaSenha(registerDTO.senha());

        Usuario usuario = new Usuario(registerDTO.login(), senhaGerada, registerDTO.role(),registerDTO.nome());

        usuarioRepository.save(usuario);

        logger.info("Usuário registrado com sucesso: {}", usuario.getLogin());

    }

    public void excluirUsuario(String email) {
        logger.info("Iniciando processo de exclusão de usuário");

        Usuario usuario = usuarioRepository.findByLoginUsuario(email);
        if (usuario == null) {
            logger.warn("Usuário não encontrado para o email: {}", email);
            throw new UsuarioException("Usuário não encontrado");
        }
        usuarioRepository.delete(usuario);
        logger.info("Usuário excluído com sucesso: {}", email);
    }

    public UsuarioListagemDTO findByEmail(String email) {
        logger.info("Buscando usuário para o email: {}", email);
        Usuario usuario = usuarioRepository.findByLoginUsuario(email);
        if (usuario == null) {
            logger.warn("Usuário não encontrado na tabela usuarios para o email: {}", email);
            return null;
        }
        return new UsuarioListagemDTO(usuario);
    }

    public UsuarioListagemDTO atualizarUsuario(String email, UsuarioListagemDTO dados) {
        logger.info("Iniciando atualização do usuário localizado por: {}", email);

        Usuario usuario = usuarioRepository.findByLoginUsuario(email);
        if (usuario == null) {
            logger.warn("Usuário não encontrado na tabela usuarios para o email: {}", email);
            throw new UsuarioException("Usuário não encontrado na tabela usuarios: " + email);
        }

        if (dados.login() != null && !dados.login().isBlank() && !dados.login().equalsIgnoreCase(email)) {
            Usuario usuarioComNovoLogin = usuarioRepository.findByLoginUsuario(dados.login());
            if (usuarioComNovoLogin != null) {
                logger.warn("Já existe usuário com o login informado para atualização: {}", dados.login());
                throw new UsuarioException("Usuário já existe com o login: " + dados.login());
            }
        }

        // Atualiza tabela usuarios
        if (dados.nome() != null && !dados.nome().isBlank())
            usuario.setNome(dados.nome());
        if (dados.role() != null)
            usuario.setRole(UserRole.valueOf(dados.role()));
        if (dados.login() != null && !dados.login().isBlank())
            usuario.setLogin(dados.login());
        usuarioRepository.save(usuario);

        logger.info("Usuário atualizado com sucesso: {}", dados.login());
        return new UsuarioListagemDTO(usuario);
    }

    public List<Usuario> findAll() {
        logger.info("Lista de  usuários ");
        List<Usuario> listaUsuario = usuarioRepository.findAll();
        if (listaUsuario.isEmpty()) {
            logger.warn("Lista de usuários vazia");
             throw new UsuarioException("Lista de usuários vazia");
        }
        return listaUsuario;
    }

}

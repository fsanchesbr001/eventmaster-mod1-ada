package com.fabriciosanches.userservice.dtos;

import com.fabriciosanches.userservice.domain.Usuario;

public record UsuarioListagemDTO(

        String nome,
        String role,
        String login
) {


    public UsuarioListagemDTO(Usuario usuario) {
        this(
                usuario != null ? usuario.getNome() : null,
                usuario != null && usuario.getRole() != null ? usuario.getRole().name() : null,
                usuario != null && usuario.getLogin() != null ? usuario.getLogin() : null
        );
    }
}

package com.fabriciosanches.userservice.dtos;

import com.fabriciosanches.userservice.domain.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UsuarioListagemDTO", description = "Dados resumidos de um usuario")
public record UsuarioListagemDTO(

        @Schema(description = "Nome completo", example = "Fabricio Sanches") String nome,
        @Schema(description = "Perfil do usuario", example = "ADMIN") String role,
        @Schema(description = "Login/email", example = "admin@eventmaster.com") String login
) {


    public UsuarioListagemDTO(Usuario usuario) {
        this(
                usuario != null ? usuario.getNome() : null,
                usuario != null && usuario.getRole() != null ? usuario.getRole().name() : null,
                usuario != null && usuario.getLogin() != null ? usuario.getLogin() : null
        );
    }
}

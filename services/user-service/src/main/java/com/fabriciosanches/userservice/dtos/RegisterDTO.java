package com.fabriciosanches.userservice.dtos;

import com.fabriciosanches.userservice.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "RegisterDTO", description = "Payload para cadastro de um novo usuario")
public record RegisterDTO(
        @Schema(description = "Login/email do usuario", example = "admin@eventmaster.com") String login,
        @Schema(description = "Senha do usuario", example = "Senha@123") String senha,
        @Schema(description = "Perfil de acesso", example = "ADMIN") UserRole role,
        @Schema(description = "Nome completo do usuario", example = "Fabricio Sanches") String nome,
        @Schema(description = "CPF do usuario", example = "12345678900") String cpf
) {

    public RegisterDTO {
        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException("Login não pode ser nulo ou vazio.");
        }
        if (senha == null || senha.isBlank()) {
            throw new IllegalArgumentException("Senha não pode ser nula ou vazia.");
        }
        if (role == null) {
            throw new IllegalArgumentException("Role não pode ser nula.");
        }
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome não pode ser nulo ou vazio.");
        }
        if (cpf == null || cpf.isBlank()) {
            throw new IllegalArgumentException("CPF não pode ser nulo ou vazio.");
        }
    }
}

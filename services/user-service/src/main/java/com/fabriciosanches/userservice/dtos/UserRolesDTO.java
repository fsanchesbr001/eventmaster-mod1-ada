package com.fabriciosanches.userservice.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "UserRolesDTO", description = "Envelope com as roles disponiveis")
public record UserRolesDTO(
		@Schema(description = "Lista de perfis de acesso disponiveis") List<RoleOptionDTO> roles
) { }

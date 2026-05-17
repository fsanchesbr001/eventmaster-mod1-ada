package com.fabriciosanches.orderservice.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "EventResponseDTO", description = "Resumo do evento consultado pelo order-service")
public record EventResponseDTO(
        @Schema(description = "Identificador do evento", example = "1")
        Long id,
        @Schema(description = "Nome do evento", example = "Show da ADA")
        String nome,
        @Schema(description = "Data do evento", example = "2026-12-10")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
        LocalDate data,
        @Schema(description = "Hora do evento", example = "20:00:00")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
        LocalTime hora,
        @Schema(description = "Preço base do evento", example = "89.90")
        BigDecimal precoBase
) {
}


package com.fabriciosanches.eventservice.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Schema(name = "EventRequestDTO", description = "Payload para criação ou atualização de um evento")
public record EventRequestDTO(
        @Schema(description = "Nome do evento", example = "Show da ADA")
        String nome,
        @Schema(description = "Data do evento no formato dd/MM/yyyy", example = "10/12/2026", type = "string")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
        LocalDate data,
        @Schema(description = "Hora do evento no formato HH:mm:ss", example = "20:00:00", type = "string")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
        LocalTime hora,
        @Schema(description = "Nome do local do evento", example = "Teatro Central")
        String local,
        @Schema(description = "Capacidade total de pessoas no local", example = "1500.0")
        Double capacidade,
        @Schema(description = "Preço base do evento", example = "89.90")
        BigDecimal precoBase
) {
}


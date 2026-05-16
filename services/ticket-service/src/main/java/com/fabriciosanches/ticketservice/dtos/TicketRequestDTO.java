package com.fabriciosanches.ticketservice.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Schema(name = "TicketRequestDTO", description = "Payload para criação ou atualização de um ingresso")
public record TicketRequestDTO(
        @Schema(description = "Id único do ingresso", example = "100001")
        Long idIngresso,
        @Schema(description = "Nome do evento", example = "Show da ADA")
        String evento,
        @Schema(description = "Data do evento no formato dd/MM/yyyy", example = "10/12/2026", type = "string")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
        LocalDate data,
        @Schema(description = "Hora do evento no formato HH:mm:ss", example = "20:00:00", type = "string")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
        LocalTime hora,
        @Schema(description = "Tipo do ingresso: Meia ou Inteira", example = "Inteira")
        String tipoIngresso,
        @Schema(description = "Valor do ingresso", example = "120.50")
        BigDecimal valor,
        @Schema(description = "Nome completo do participante", example = "Fabricio Sanches")
        String nomeParticipante,
        @Schema(description = "CPF do participante", example = "123.456.789-00")
        String cpfParticipante,
        @Schema(description = "Situação do ingresso. Quando não informada, assume Disponivel", example = "Disponivel")
        String situacao
) {
}


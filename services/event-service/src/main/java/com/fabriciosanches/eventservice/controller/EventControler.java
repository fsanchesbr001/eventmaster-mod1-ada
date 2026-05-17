package com.fabriciosanches.eventservice.controller;

import com.fabriciosanches.eventservice.dtos.ApiErrorResponseDTO;
import com.fabriciosanches.eventservice.dtos.EventRequestDTO;
import com.fabriciosanches.eventservice.dtos.EventResponseDTO;
import com.fabriciosanches.eventservice.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/event-service/eventos")
@Tag(name = "Eventos", description = "Operações de CRUD para gestão de eventos")
public class EventControler {

    private final EventService eventService;

    public EventControler(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping({"", "/"})
    @Operation(summary = "Criar evento", description = "Cria um novo evento com nome, data, hora, local e capacidade.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Dados do novo evento",
            content = @Content(
                    schema = @Schema(implementation = EventRequestDTO.class),
                    examples = @ExampleObject(name = "novoEvento", value = "{\"nome\":\"Show da ADA\",\"data\":\"10/12/2026\",\"hora\":\"20:00:00\",\"local\":\"Teatro Central\",\"capacidade\":1500.0,\"precoBase\":89.90}")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Evento criado com sucesso", content = @Content(
                    schema = @Schema(implementation = EventResponseDTO.class),
                    examples = @ExampleObject(value = "{\"id\":1,\"nome\":\"Show da ADA\",\"data\":\"10/12/2026\",\"hora\":\"20:00:00\",\"local\":\"Teatro Central\",\"capacidade\":1500.0,\"precoBase\":89.90}"))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(
                    schema = @Schema(implementation = ApiErrorResponseDTO.class),
                    examples = @ExampleObject(value = "{\"error\":\"BAD_REQUEST\",\"message\":\"Nome do evento é obrigatório\"}"))),
            @ApiResponse(responseCode = "500", description = "Erro interno", content = @Content(
                    schema = @Schema(implementation = ApiErrorResponseDTO.class),
                    examples = @ExampleObject(value = "{\"error\":\"INTERNAL_SERVER_ERROR\",\"message\":\"Erro interno ao processar a requisição\"}")))
    })
    public ResponseEntity<EventResponseDTO> criar(@RequestBody EventRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.criar(request));
    }

    @GetMapping({"", "/"})
    @Operation(summary = "Listar eventos", description = "Retorna todos os eventos cadastrados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso", content = @Content(
                    array = @ArraySchema(schema = @Schema(implementation = EventResponseDTO.class)),
                    examples = @ExampleObject(value = "[{\"id\":1,\"nome\":\"Show da ADA\",\"data\":\"10/12/2026\",\"hora\":\"20:00:00\",\"local\":\"Teatro Central\",\"capacidade\":1500.0,\"precoBase\":89.90}]"))),
            @ApiResponse(responseCode = "500", description = "Erro interno", content = @Content(
                    schema = @Schema(implementation = ApiErrorResponseDTO.class)))
    })
    public ResponseEntity<List<EventResponseDTO>> listarTodos() {
        return ResponseEntity.ok(eventService.listarTodos());
    }

    @GetMapping({"/{id}", "/{id}/"})
    @Operation(summary = "Buscar evento por id", description = "Retorna os dados de um evento específico.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Evento encontrado", content = @Content(
                    schema = @Schema(implementation = EventResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado", content = @Content(
                    schema = @Schema(implementation = ApiErrorResponseDTO.class),
                    examples = @ExampleObject(value = "{\"error\":\"NOT_FOUND\",\"message\":\"Evento não encontrado para o id: 99\"}")))
    })
    public ResponseEntity<EventResponseDTO> buscarPorId(
            @Parameter(description = "Identificador do evento", example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(eventService.buscarPorId(id));
    }

    @PutMapping({"/{id}", "/{id}/"})
    @Operation(summary = "Atualizar evento", description = "Atualiza todos os dados de um evento existente.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Novos dados do evento",
            content = @Content(
                    schema = @Schema(implementation = EventRequestDTO.class),
                    examples = @ExampleObject(name = "eventoAtualizado", value = "{\"nome\":\"Peça de Teatro Premium\",\"data\":\"12/12/2026\",\"hora\":\"21:30:00\",\"local\":\"Auditório Principal\",\"capacidade\":950.0,\"precoBase\":120.00}")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Evento atualizado com sucesso", content = @Content(
                    schema = @Schema(implementation = EventResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(
                    schema = @Schema(implementation = ApiErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado", content = @Content(
                    schema = @Schema(implementation = ApiErrorResponseDTO.class)))
    })
    public ResponseEntity<EventResponseDTO> atualizar(
            @Parameter(description = "Identificador do evento", example = "1")
            @PathVariable Long id,
            @RequestBody EventRequestDTO request) {
        return ResponseEntity.ok(eventService.atualizar(id, request));
    }

    @DeleteMapping({"/{id}", "/{id}/"})
    @Operation(summary = "Excluir evento", description = "Remove um evento pelo identificador informado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Evento excluído com sucesso"),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado", content = @Content(
                    schema = @Schema(implementation = ApiErrorResponseDTO.class)))
    })
    public ResponseEntity<Void> excluir(
            @Parameter(description = "Identificador do evento", example = "1")
            @PathVariable Long id) {
        eventService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}


package com.fabriciosanches.ticketservice.controller;

import com.fabriciosanches.ticketservice.dtos.ApiErrorResponseDTO;
import com.fabriciosanches.ticketservice.dtos.ReservaRequestDTO;
import com.fabriciosanches.ticketservice.dtos.TicketRequestDTO;
import com.fabriciosanches.ticketservice.dtos.TicketResponseDTO;
import com.fabriciosanches.ticketservice.service.TicketService;
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
@RequestMapping("/ticket-service/ingressos")
@Tag(name = "Ingressos", description = "Operações de CRUD para gestão de ingressos")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping({"", "/"})
    @Operation(summary = "Criar ingresso", description = "Cria um novo ingresso com dados do evento e do participante.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Dados do novo ingresso",
            content = @Content(
                    schema = @Schema(implementation = TicketRequestDTO.class),
                    examples = @ExampleObject(name = "novoIngresso", value = "{\"idIngresso\":100001,\"evento\":\"Show da ADA\",\"data\":\"10/12/2026\",\"hora\":\"20:00:00\",\"tipoIngresso\":\"Inteira\",\"valor\":120.50,\"nomeParticipante\":\"Fabricio Sanches\",\"cpfParticipante\":\"123.456.789-00\",\"situacao\":\"Disponivel\"}")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Ingresso criado com sucesso", content = @Content(
                    schema = @Schema(implementation = TicketResponseDTO.class),
                    examples = @ExampleObject(value = "{\"id\":1,\"idIngresso\":100001,\"evento\":\"Show da ADA\",\"data\":\"10/12/2026\",\"hora\":\"20:00:00\",\"tipoIngresso\":\"Inteira\",\"valor\":120.50,\"nomeParticipante\":\"Fabricio Sanches\",\"cpfParticipante\":\"123.456.789-00\",\"situacao\":\"Disponivel\"}"))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(
                    schema = @Schema(implementation = ApiErrorResponseDTO.class),
                    examples = @ExampleObject(value = "{\"error\":\"BAD_REQUEST\",\"message\":\"Tipo do ingresso deve ser Meia ou Inteira\"}"))),
            @ApiResponse(responseCode = "500", description = "Erro interno", content = @Content(
                    schema = @Schema(implementation = ApiErrorResponseDTO.class),
                    examples = @ExampleObject(value = "{\"error\":\"INTERNAL_SERVER_ERROR\",\"message\":\"Erro interno ao processar a requisição\"}")))
    })
    public ResponseEntity<TicketResponseDTO> criar(@RequestBody TicketRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.criar(request));
    }

    @GetMapping({"", "/"})
    @Operation(summary = "Listar ingressos", description = "Retorna todos os ingressos cadastrados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso", content = @Content(
                    array = @ArraySchema(schema = @Schema(implementation = TicketResponseDTO.class)),
                    examples = @ExampleObject(value = "[{\"id\":1,\"idIngresso\":100001,\"evento\":\"Show da ADA\",\"data\":\"10/12/2026\",\"hora\":\"20:00:00\",\"tipoIngresso\":\"Inteira\",\"valor\":120.50,\"nomeParticipante\":\"Fabricio Sanches\",\"cpfParticipante\":\"123.456.789-00\",\"situacao\":\"Disponivel\"}]"))),
            @ApiResponse(responseCode = "500", description = "Erro interno", content = @Content(
                    schema = @Schema(implementation = ApiErrorResponseDTO.class)))
    })
    public ResponseEntity<List<TicketResponseDTO>> listarTodos() {
        return ResponseEntity.ok(ticketService.listarTodos());
    }

    @PostMapping({"/reserva", "/reserva/"})
    @Operation(summary = "Reservar ingressos no Redis", description = "Reserva uma quantidade de ingressos do evento no Redis para uso transacional pelo order-service.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Evento e quantidade a reservar",
            content = @Content(
                    schema = @Schema(implementation = ReservaRequestDTO.class),
                    examples = @ExampleObject(name = "reserva", value = "{\"eventId\":1,\"quantidade\":2}")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Reserva efetuada com sucesso"),
            @ApiResponse(responseCode = "409", description = "Sem estoque disponível", content = @Content(
                    schema = @Schema(implementation = ApiErrorResponseDTO.class),
                    examples = @ExampleObject(value = "{\"error\":\"CONFLICT\",\"message\":\"Ingressos indisponíveis ou esgotados para o evento informado\"}")))
    })
    public ResponseEntity<Void> reservar(@RequestBody ReservaRequestDTO request) {
        ticketService.reservarIngressos(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping({"/{id}", "/{id}/"})
    @Operation(summary = "Buscar ingresso por id", description = "Retorna os dados de um ingresso específico.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ingresso encontrado", content = @Content(
                    schema = @Schema(implementation = TicketResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Ingresso não encontrado", content = @Content(
                    schema = @Schema(implementation = ApiErrorResponseDTO.class),
                    examples = @ExampleObject(value = "{\"error\":\"NOT_FOUND\",\"message\":\"Ingresso não encontrado para o id: 99\"}")))
    })
    public ResponseEntity<TicketResponseDTO> buscarPorId(
            @Parameter(description = "Identificador interno do ingresso", example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(ticketService.buscarPorId(id));
    }

    @PutMapping({"/{id}", "/{id}/"})
    @Operation(summary = "Atualizar ingresso", description = "Atualiza todos os dados de um ingresso existente.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Novos dados do ingresso",
            content = @Content(
                    schema = @Schema(implementation = TicketRequestDTO.class),
                    examples = @ExampleObject(name = "ingressoAtualizado", value = "{\"idIngresso\":100001,\"evento\":\"Peça de Teatro Premium\",\"data\":\"12/12/2026\",\"hora\":\"21:30:00\",\"tipoIngresso\":\"Meia\",\"valor\":75.00,\"nomeParticipante\":\"Fabricio Sanches\",\"cpfParticipante\":\"123.456.789-00\",\"situacao\":\"Reservado\"}")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ingresso atualizado com sucesso", content = @Content(
                    schema = @Schema(implementation = TicketResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(
                    schema = @Schema(implementation = ApiErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Ingresso não encontrado", content = @Content(
                    schema = @Schema(implementation = ApiErrorResponseDTO.class)))
    })
    public ResponseEntity<TicketResponseDTO> atualizar(
            @Parameter(description = "Identificador interno do ingresso", example = "1")
            @PathVariable Long id,
            @RequestBody TicketRequestDTO request) {
        return ResponseEntity.ok(ticketService.atualizar(id, request));
    }

    @DeleteMapping({"/{id}", "/{id}/"})
    @Operation(summary = "Excluir ingresso", description = "Remove um ingresso pelo identificador informado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Ingresso excluído com sucesso"),
            @ApiResponse(responseCode = "404", description = "Ingresso não encontrado", content = @Content(
                    schema = @Schema(implementation = ApiErrorResponseDTO.class)))
    })
    public ResponseEntity<Void> excluir(
            @Parameter(description = "Identificador interno do ingresso", example = "1")
            @PathVariable Long id) {
        ticketService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}


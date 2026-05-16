package com.fabriciosanches.orderservice.controller;

import com.fabriciosanches.orderservice.dtos.ApiErrorResponseDTO;
import com.fabriciosanches.orderservice.dtos.OrderRequestDTO;
import com.fabriciosanches.orderservice.dtos.OrderResponseDTO;
import com.fabriciosanches.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/order-service/pedidos")
@Tag(name = "Pedidos", description = "Operações de criação de pedidos do EventMaster")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(summary = "Criar pedido", description = "Cria um pedido, reserva o estoque no Redis via ticket-service e publica PEDIDO_REALIZADO no Kafka.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Dados do pedido",
            content = @Content(
                    schema = @Schema(implementation = OrderRequestDTO.class),
                    examples = @ExampleObject(name = "novoPedido", value = "{\"eventId\":1,\"formaPagamento\":\"PIX\",\"itens\":[{\"nomePortador\":\"Fabricio Sanches\",\"cpfPortador\":\"123.456.789-00\",\"tipoIngresso\":\"INTEIRA\"},{\"nomePortador\":\"Ada Lovelace\",\"cpfPortador\":\"987.654.321-00\",\"tipoIngresso\":\"MEIA\"}]}")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Pedido criado com sucesso", content = @Content(
                    schema = @Schema(implementation = OrderResponseDTO.class),
                    examples = @ExampleObject(value = "{\"id\":1,\"valorTotal\":134.85,\"status\":\"REALIZADO\"}"))),
            @ApiResponse(responseCode = "400", description = "Pedido inválido", content = @Content(
                    schema = @Schema(implementation = ApiErrorResponseDTO.class),
                    examples = @ExampleObject(value = "{\"error\":\"BAD_REQUEST\",\"message\":\"Forma de pagamento é obrigatória\"}"))),
            @ApiResponse(responseCode = "409", description = "Falha de integração ou estoque insuficiente", content = @Content(
                    schema = @Schema(implementation = ApiErrorResponseDTO.class),
                    examples = @ExampleObject(value = "{\"error\":\"CONFLICT\",\"message\":\"Ingressos indisponíveis ou esgotados no Redis para o evento informado.\"}")))
    })
    public ResponseEntity<OrderResponseDTO> criar(
            @RequestBody OrderRequestDTO dto,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            Authentication authentication) {
        OrderResponseDTO resposta = orderService.criarPedido(dto, authentication.getName(), authorizationHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }
}


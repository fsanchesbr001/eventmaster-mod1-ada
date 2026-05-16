package com.fabriciosanches.ticketservice.service;

import com.fabriciosanches.shared.events.PedidoCanceladoEvent;
import com.fabriciosanches.shared.events.PedidoConfirmadoEvent;
import com.fabriciosanches.shared.events.PedidoRealizadoEvent;
import com.fabriciosanches.shared.events.PedidoRealizadoItemEvent;
import com.fabriciosanches.ticketservice.domain.Ticket;
import com.fabriciosanches.ticketservice.dtos.ReservaRequestDTO;
import com.fabriciosanches.ticketservice.dtos.TicketRequestDTO;
import com.fabriciosanches.ticketservice.dtos.TicketResponseDTO;
import com.fabriciosanches.ticketservice.exceptions.TicketInventoryException;
import com.fabriciosanches.ticketservice.exceptions.TicketNotFoundException;
import com.fabriciosanches.ticketservice.exceptions.TicketValidationException;
import com.fabriciosanches.ticketservice.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class TicketService {

    private static final Logger logger = LoggerFactory.getLogger(TicketService.class);
    private static final Set<String> TIPOS_INGRESSO_VALIDOS = Set.of("Meia", "Inteira");
    private static final String SITUACAO_PADRAO = "Disponivel";
    private static final String SITUACAO_RESERVADO = "Reservado";
    private static final String SITUACAO_CONFIRMADO = "Confirmado";

    private final TicketRepository ticketRepository;
    private final EventInventoryService eventInventoryService;

    public TicketService(TicketRepository ticketRepository, EventInventoryService eventInventoryService) {
        this.ticketRepository = ticketRepository;
        this.eventInventoryService = eventInventoryService;
    }

    @Transactional
    public TicketResponseDTO criar(TicketRequestDTO request) {
        logger.info("Criando ingresso com idIngresso='{}' para o evento='{}'", request.idIngresso(), request.evento());
        TicketRequestDTO normalized = validarEPadronizar(request, null);

        Ticket ticket = new Ticket(
                normalized.idIngresso(),
                normalized.evento(),
                normalized.data(),
                normalized.hora(),
                normalized.tipoIngresso(),
                normalized.valor(),
                normalized.nomeParticipante(),
                normalized.cpfParticipante(),
                normalized.situacao()
        );

        Ticket saved = ticketRepository.save(ticket);
        return new TicketResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<TicketResponseDTO> listarTodos() {
        logger.info("Listando todos os ingressos cadastrados");
        return ticketRepository.findAll()
                .stream()
                .map(TicketResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public TicketResponseDTO buscarPorId(Long id) {
        logger.info("Buscando ingresso por id={}", id);
        return new TicketResponseDTO(obterEntidade(id));
    }

    @Transactional
    public TicketResponseDTO atualizar(Long id, TicketRequestDTO request) {
        logger.info("Atualizando ingresso id={}", id);
        Ticket ticket = obterEntidade(id);
        TicketRequestDTO normalized = validarEPadronizar(request, id);

        ticket.setIdIngresso(normalized.idIngresso());
        ticket.setEvento(normalized.evento());
        ticket.setData(normalized.data());
        ticket.setHora(normalized.hora());
        ticket.setTipoIngresso(normalized.tipoIngresso());
        ticket.setValor(normalized.valor());
        ticket.setNomeParticipante(normalized.nomeParticipante());
        ticket.setCpfParticipante(normalized.cpfParticipante());
        ticket.setSituacao(normalized.situacao());

        Ticket updated = ticketRepository.save(ticket);
        return new TicketResponseDTO(updated);
    }

    @Transactional
    public void excluir(Long id) {
        logger.info("Excluindo ingresso id={}", id);
        Ticket ticket = obterEntidade(id);
        ticketRepository.delete(ticket);
    }

    public void reservarIngressos(ReservaRequestDTO request) {
        if (request == null) {
            throw new TicketInventoryException("Payload de reserva é obrigatório");
        }
        logger.info("Reservando {} ingressos para o evento id={}", request.quantidade(), request.eventId());
        eventInventoryService.reservarIngressos(request.eventId(), request.quantidade());
    }

    @Transactional
    public void registrarPedidoRealizado(PedidoRealizadoEvent event) {
        validarPedidoRealizado(event);

        if (ticketRepository.existsByPedidoId(event.pedidoId())) {
            logger.info("Pedido id={} já foi materializado no banco de ingressos. Ignorando reprocessamento.", event.pedidoId());
            return;
        }

        materializarIngressosDoPedido(
                event.pedidoId(),
                event.eventoNome(),
                event.dataEvento(),
                event.horaEvento(),
                event.itens(),
                SITUACAO_RESERVADO
        );
    }

    @Transactional
    public void confirmarPedido(PedidoConfirmadoEvent event) {
        validarEventoFinal(event.pedidoId(), event.eventoId(), event.eventoNome(), event.dataEvento(), event.horaEvento(), event.valorTotal(), event.itens(), "confirmado");

        List<Ticket> tickets = ticketRepository.findByPedidoId(event.pedidoId());
        if (tickets.isEmpty()) {
            logger.warn("Pedido confirmado id={} chegou antes da materialização do pedido realizado. Criando ingressos já confirmados.", event.pedidoId());
            materializarIngressosDoPedido(event.pedidoId(), event.eventoNome(), event.dataEvento(), event.horaEvento(), event.itens(), SITUACAO_CONFIRMADO);
            return;
        }
        if (tickets.stream().allMatch(ticket -> SITUACAO_CONFIRMADO.equals(ticket.getSituacao()))) {
            logger.info("Ingressos do pedido id={} já estão confirmados. Ignorando reprocessamento.", event.pedidoId());
            return;
        }
        if (tickets.stream().anyMatch(ticket -> SITUACAO_PADRAO.equals(ticket.getSituacao()))) {
            logger.warn("Ingressos do pedido id={} já estão disponíveis após cancelamento. Evento de confirmação será ignorado.", event.pedidoId());
            return;
        }

        tickets.forEach(ticket -> ticket.setSituacao(SITUACAO_CONFIRMADO));
        ticketRepository.saveAll(tickets);
    }

    @Transactional
    public void cancelarPedido(PedidoCanceladoEvent event) {
        validarEventoFinal(event.pedidoId(), event.eventoId(), event.eventoNome(), event.dataEvento(), event.horaEvento(), event.valorTotal(), event.itens(), "cancelado");

        List<Ticket> tickets = ticketRepository.findByPedidoId(event.pedidoId());
        if (tickets.isEmpty()) {
            logger.warn("Pedido cancelado id={} chegou antes da materialização do pedido realizado. Criando ingressos disponíveis e estornando estoque.", event.pedidoId());
            eventInventoryService.devolverIngressos(event.eventoId(), event.itens().size());
            materializarIngressosDoPedido(event.pedidoId(), event.eventoNome(), event.dataEvento(), event.horaEvento(), event.itens(), SITUACAO_PADRAO);
            return;
        }
        if (tickets.stream().allMatch(ticket -> SITUACAO_PADRAO.equals(ticket.getSituacao()))) {
            logger.info("Ingressos do pedido id={} já estão disponíveis. Ignorando reprocessamento do cancelamento.", event.pedidoId());
            return;
        }
        if (tickets.stream().anyMatch(ticket -> SITUACAO_CONFIRMADO.equals(ticket.getSituacao()))) {
            logger.warn("Ingressos do pedido id={} já foram confirmados. Evento de cancelamento será ignorado.", event.pedidoId());
            return;
        }

        long quantidadeParaDevolver = tickets.stream()
                .filter(ticket -> !SITUACAO_PADRAO.equals(ticket.getSituacao()))
                .count();
        if (quantidadeParaDevolver > 0) {
            eventInventoryService.devolverIngressos(event.eventoId(), Math.toIntExact(quantidadeParaDevolver));
        }
        tickets.forEach(ticket -> ticket.setSituacao(SITUACAO_PADRAO));
        ticketRepository.saveAll(tickets);
    }

    private Ticket obterEntidade(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException("Ingresso não encontrado para o id: " + id));
    }

    private void validarPedidoRealizado(PedidoRealizadoEvent event) {
        if (event == null) {
            throw new TicketValidationException("Evento de pedido realizado é obrigatório");
        }
        if (event.eventoId() == null || event.eventoId() <= 0) {
            throw new TicketValidationException("Identificador do evento do pedido realizado é obrigatório");
        }
        if (event.pedidoId() == null || event.pedidoId() <= 0) {
            throw new TicketValidationException("Identificador do pedido realizado é obrigatório");
        }
        if (event.eventoNome() == null || event.eventoNome().isBlank()) {
            throw new TicketValidationException("Nome do evento do pedido realizado é obrigatório");
        }
        if (event.dataEvento() == null) {
            throw new TicketValidationException("Data do evento do pedido realizado é obrigatória");
        }
        if (event.horaEvento() == null) {
            throw new TicketValidationException("Hora do evento do pedido realizado é obrigatória");
        }
        if (event.itens() == null || event.itens().isEmpty()) {
            throw new TicketValidationException("Pedido realizado deve conter ao menos um item");
        }
        for (PedidoRealizadoItemEvent item : event.itens()) {
            if (item == null) {
                throw new TicketValidationException("Itens do pedido realizado devem ser válidos");
            }
            if (item.nomePortador() == null || item.nomePortador().isBlank()) {
                throw new TicketValidationException("Nome do portador do pedido realizado é obrigatório");
            }
            if (item.cpfPortador() == null || item.cpfPortador().isBlank()) {
                throw new TicketValidationException("CPF do portador do pedido realizado é obrigatório");
            }
            if (item.tipoIngresso() == null || item.tipoIngresso().isBlank()) {
                throw new TicketValidationException("Tipo do ingresso do pedido realizado é obrigatório");
            }
            if (item.precoPago() == null || item.precoPago().compareTo(BigDecimal.ZERO) <= 0) {
                throw new TicketValidationException("Preço pago do item do pedido realizado deve ser maior que zero");
            }
        }
    }

    private void validarEventoFinal(Long pedidoId,
                                    Long eventoId,
                                    String eventoNome,
                                    java.time.LocalDate dataEvento,
                                    java.time.LocalTime horaEvento,
                                    BigDecimal valorTotal,
                                    List<PedidoRealizadoItemEvent> itens,
                                    String tipoEvento) {
        if (pedidoId == null || pedidoId <= 0) {
            throw new TicketValidationException("Evento de pedido " + tipoEvento + " precisa informar um pedido válido");
        }
        if (eventoId == null || eventoId <= 0) {
            throw new TicketValidationException("Evento de pedido " + tipoEvento + " precisa informar um evento válido");
        }
        if (eventoNome == null || eventoNome.isBlank()) {
            throw new TicketValidationException("Evento de pedido " + tipoEvento + " precisa informar o nome do evento");
        }
        if (dataEvento == null) {
            throw new TicketValidationException("Evento de pedido " + tipoEvento + " precisa informar a data do evento");
        }
        if (horaEvento == null) {
            throw new TicketValidationException("Evento de pedido " + tipoEvento + " precisa informar a hora do evento");
        }
        if (valorTotal == null || valorTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TicketValidationException("Evento de pedido " + tipoEvento + " precisa informar o valor total do pedido");
        }
        if (itens == null || itens.isEmpty()) {
            throw new TicketValidationException("Evento de pedido " + tipoEvento + " precisa informar ao menos um item");
        }
        for (PedidoRealizadoItemEvent item : itens) {
            if (item == null) {
                throw new TicketValidationException("Itens do pedido " + tipoEvento + " devem ser válidos");
            }
            if (item.nomePortador() == null || item.nomePortador().isBlank()) {
                throw new TicketValidationException("Nome do portador do pedido " + tipoEvento + " é obrigatório");
            }
            if (item.cpfPortador() == null || item.cpfPortador().isBlank()) {
                throw new TicketValidationException("CPF do portador do pedido " + tipoEvento + " é obrigatório");
            }
            if (item.tipoIngresso() == null || item.tipoIngresso().isBlank()) {
                throw new TicketValidationException("Tipo do ingresso do pedido " + tipoEvento + " é obrigatório");
            }
            if (item.precoPago() == null || item.precoPago().compareTo(BigDecimal.ZERO) <= 0) {
                throw new TicketValidationException("Preço pago do item do pedido " + tipoEvento + " deve ser maior que zero");
            }
        }
    }

    private void materializarIngressosDoPedido(Long pedidoId,
                                               String eventoNome,
                                               java.time.LocalDate dataEvento,
                                               java.time.LocalTime horaEvento,
                                               List<PedidoRealizadoItemEvent> itens,
                                               String situacao) {
        long proximoIdIngresso = ticketRepository.findTopByOrderByIdIngressoDesc()
                .map(ticket -> ticket.getIdIngresso() + 1)
                .orElse(1L);

        for (PedidoRealizadoItemEvent item : itens) {
            String tipoIngressoNormalizado = normalizarTipoIngresso(item.tipoIngresso());
            if (!TIPOS_INGRESSO_VALIDOS.contains(tipoIngressoNormalizado)) {
                throw new TicketValidationException("Tipo do ingresso do pedido realizado deve ser Meia ou Inteira");
            }

            Ticket ticket = new Ticket(
                    proximoIdIngresso++,
                    eventoNome.trim(),
                    dataEvento,
                    horaEvento,
                    tipoIngressoNormalizado,
                    item.precoPago().setScale(2, RoundingMode.HALF_UP),
                    item.nomePortador().trim(),
                    item.cpfPortador().trim(),
                    situacao
            );
            ticket.setPedidoId(pedidoId);
            ticketRepository.save(ticket);
        }
    }

    private TicketRequestDTO validarEPadronizar(TicketRequestDTO request, Long currentId) {
        if (request == null) {
            throw new TicketValidationException("Payload do ingresso é obrigatório");
        }
        if (request.idIngresso() == null || request.idIngresso() <= 0) {
            throw new TicketValidationException("Id do ingresso é obrigatório e deve ser maior que zero");
        }
        if (currentId == null && ticketRepository.existsByIdIngresso(request.idIngresso())) {
            throw new TicketValidationException("Já existe um ingresso cadastrado com o idIngresso informado");
        }
        if (currentId != null && ticketRepository.existsByIdIngressoAndIdNot(request.idIngresso(), currentId)) {
            throw new TicketValidationException("Já existe outro ingresso cadastrado com o idIngresso informado");
        }
        if (request.evento() == null || request.evento().isBlank()) {
            throw new TicketValidationException("Nome do evento é obrigatório");
        }
        if (request.data() == null) {
            throw new TicketValidationException("Data do evento é obrigatória");
        }
        if (request.hora() == null) {
            throw new TicketValidationException("Hora do evento é obrigatória");
        }
        if (request.tipoIngresso() == null || request.tipoIngresso().isBlank()) {
            throw new TicketValidationException("Tipo do ingresso é obrigatório");
        }
        String tipoIngressoNormalizado = normalizarTipoIngresso(request.tipoIngresso());
        if (!TIPOS_INGRESSO_VALIDOS.contains(tipoIngressoNormalizado)) {
            throw new TicketValidationException("Tipo do ingresso deve ser Meia ou Inteira");
        }
        if (request.valor() == null) {
            throw new TicketValidationException("Valor do ingresso é obrigatório");
        }
        if (request.valor().compareTo(BigDecimal.ZERO) <= 0) {
            throw new TicketValidationException("Valor do ingresso deve ser maior que zero");
        }
        if (request.nomeParticipante() == null || request.nomeParticipante().isBlank()) {
            throw new TicketValidationException("Nome do participante é obrigatório");
        }
        if (request.cpfParticipante() == null || request.cpfParticipante().isBlank()) {
            throw new TicketValidationException("CPF do participante é obrigatório");
        }
        String situacaoNormalizada = normalizarSituacao(request.situacao());
        if (situacaoNormalizada.length() > 20) {
            throw new TicketValidationException("Situação do ingresso deve ter no máximo 20 caracteres");
        }

        return new TicketRequestDTO(
                request.idIngresso(),
                request.evento().trim(),
                request.data(),
                request.hora(),
                tipoIngressoNormalizado,
                request.valor().setScale(2, RoundingMode.HALF_UP),
                request.nomeParticipante().trim(),
                request.cpfParticipante().trim(),
                situacaoNormalizada
        );
    }

    private String normalizarTipoIngresso(String tipoIngresso) {
        String texto = tipoIngresso.trim().toLowerCase(Locale.ROOT);
        if (texto.isBlank()) {
            return texto;
        }
        return texto.substring(0, 1).toUpperCase(Locale.ROOT) + texto.substring(1);
    }

    private String normalizarSituacao(String situacao) {
        if (situacao == null || situacao.isBlank()) {
            return SITUACAO_PADRAO;
        }
        return situacao.trim();
    }
}


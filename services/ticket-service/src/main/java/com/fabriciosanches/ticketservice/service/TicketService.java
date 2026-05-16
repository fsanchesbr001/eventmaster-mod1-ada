package com.fabriciosanches.ticketservice.service;

import com.fabriciosanches.ticketservice.domain.Ticket;
import com.fabriciosanches.ticketservice.dtos.TicketRequestDTO;
import com.fabriciosanches.ticketservice.dtos.TicketResponseDTO;
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

    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
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

    private Ticket obterEntidade(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException("Ingresso não encontrado para o id: " + id));
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


package com.fabriciosanches.eventservice.service;

import com.fabriciosanches.eventservice.domain.Event;
import com.fabriciosanches.eventservice.dtos.EventRequestDTO;
import com.fabriciosanches.eventservice.dtos.EventResponseDTO;
import com.fabriciosanches.eventservice.exceptions.EventNotFoundException;
import com.fabriciosanches.eventservice.exceptions.EventValidationException;
import com.fabriciosanches.eventservice.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EventService {

    private static final Logger logger = LoggerFactory.getLogger(EventService.class);

    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Transactional
    public EventResponseDTO criar(EventRequestDTO request) {
        logger.info("Criando evento com nome='{}'", request.nome());
        validar(request);

        Event event = new Event(
                request.nome().trim(),
                request.data(),
                request.hora(),
                request.local().trim(),
                request.capacidade()
        );

        Event saved = eventRepository.save(event);
        return new EventResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<EventResponseDTO> listarTodos() {
        logger.info("Listando todos os eventos cadastrados");
        return eventRepository.findAll()
                .stream()
                .map(EventResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public EventResponseDTO buscarPorId(Long id) {
        logger.info("Buscando evento por id={}", id);
        return new EventResponseDTO(obterEntidade(id));
    }

    @Transactional
    public EventResponseDTO atualizar(Long id, EventRequestDTO request) {
        logger.info("Atualizando evento id={}", id);
        validar(request);

        Event event = obterEntidade(id);
        event.setNome(request.nome().trim());
        event.setData(request.data());
        event.setHora(request.hora());
        event.setLocal(request.local().trim());
        event.setCapacidade(request.capacidade());

        Event updated = eventRepository.save(event);
        return new EventResponseDTO(updated);
    }

    @Transactional
    public void excluir(Long id) {
        logger.info("Excluindo evento id={}", id);
        Event event = obterEntidade(id);
        eventRepository.delete(event);
    }

    private Event obterEntidade(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException("Evento não encontrado para o id: " + id));
    }

    private void validar(EventRequestDTO request) {
        if (request == null) {
            throw new EventValidationException("Payload do evento é obrigatório");
        }
        if (request.nome() == null || request.nome().isBlank()) {
            throw new EventValidationException("Nome do evento é obrigatório");
        }
        if (request.data() == null) {
            throw new EventValidationException("Data do evento é obrigatória");
        }
        if (request.hora() == null) {
            throw new EventValidationException("Hora do evento é obrigatória");
        }
        if (request.local() == null || request.local().isBlank()) {
            throw new EventValidationException("Local do evento é obrigatório");
        }
        if (request.capacidade() == null) {
            throw new EventValidationException("Capacidade do evento é obrigatória");
        }
        if (request.capacidade() <= 0) {
            throw new EventValidationException("Capacidade do evento deve ser maior que zero");
        }
    }
}


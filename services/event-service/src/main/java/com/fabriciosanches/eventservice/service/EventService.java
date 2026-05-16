package com.fabriciosanches.eventservice.service;

import com.fabriciosanches.shared.events.EventCreatedEvent;
import com.fabriciosanches.eventservice.domain.Event;
import com.fabriciosanches.eventservice.dtos.EventRequestDTO;
import com.fabriciosanches.eventservice.dtos.EventResponseDTO;
import com.fabriciosanches.eventservice.exceptions.EventNotFoundException;
import com.fabriciosanches.eventservice.exceptions.EventValidationException;
import com.fabriciosanches.eventservice.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.List;

@Service
public class EventService {

    private static final Logger logger = LoggerFactory.getLogger(EventService.class);

    private final EventRepository eventRepository;
    private final KafkaTemplate<String, EventCreatedEvent> kafkaTemplate;
    private final String eventoCriadoTopic;
    private final boolean eventCreatedPublishingEnabled;

    public EventService(EventRepository eventRepository,
                        KafkaTemplate<String, EventCreatedEvent> kafkaTemplate,
                        @Value("${app.kafka.topic.evento-criado}") String eventoCriadoTopic,
                        @Value("${app.messaging.event-created.enabled:true}") boolean eventCreatedPublishingEnabled) {
        this.eventRepository = eventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.eventoCriadoTopic = eventoCriadoTopic;
        this.eventCreatedPublishingEnabled = eventCreatedPublishingEnabled;
    }

    @Transactional
    public EventResponseDTO criar(EventRequestDTO request) {
        logger.info("Criando evento com nome='{}'", request.nome());
        EventRequestDTO normalized = validarEPadronizar(request);

        Event event = new Event(
                normalized.nome(),
                normalized.data(),
                normalized.hora(),
                normalized.local(),
                normalized.capacidade(),
                normalized.precoBase()
        );

        Event saved = eventRepository.save(event);
        publicarEventoCriado(saved);
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
        EventRequestDTO normalized = validarEPadronizar(request);

        Event event = obterEntidade(id);
        event.setNome(normalized.nome());
        event.setData(normalized.data());
        event.setHora(normalized.hora());
        event.setLocal(normalized.local());
        event.setCapacidade(normalized.capacidade());
        event.setPrecoBase(normalized.precoBase());

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

    private EventRequestDTO validarEPadronizar(EventRequestDTO request) {
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
        BigDecimal precoBaseNormalizado = normalizarPrecoBase(request.precoBase());
        if (precoBaseNormalizado.compareTo(BigDecimal.ZERO) < 0) {
            throw new EventValidationException("Preço base do evento não pode ser negativo");
        }

        return new EventRequestDTO(
                request.nome().trim(),
                request.data(),
                request.hora(),
                request.local().trim(),
                request.capacidade(),
                precoBaseNormalizado
        );
    }

    private BigDecimal normalizarPrecoBase(BigDecimal precoBase) {
        return Objects.requireNonNullElse(precoBase, BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void publicarEventoCriado(Event event) {
        if (!eventCreatedPublishingEnabled) {
            logger.debug("Publicação do EVENTO_CRIADO desabilitada para o evento id={}", event.getId());
            return;
        }

        EventCreatedEvent eventCreatedEvent = new EventCreatedEvent(
                event.getId(),
                event.getNome(),
                (int) Math.floor(event.getCapacidade())
        );

        Runnable publishAction = () -> {
            logger.info("Publicando evento {} no tópico {} para o evento id={}", "EVENTO_CRIADO", eventoCriadoTopic, event.getId());
            kafkaTemplate.send(eventoCriadoTopic, String.valueOf(event.getId()), eventCreatedEvent);
        };

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishAction.run();
                }
            });
            return;
        }

        publishAction.run();
    }
}


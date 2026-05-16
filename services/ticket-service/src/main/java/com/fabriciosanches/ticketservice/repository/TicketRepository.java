package com.fabriciosanches.ticketservice.repository;

import com.fabriciosanches.ticketservice.domain.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    boolean existsByIdIngresso(Long idIngresso);

    boolean existsByPedidoId(Long pedidoId);

    boolean existsByIdIngressoAndIdNot(Long idIngresso, Long id);

    Optional<Ticket> findTopByOrderByIdIngressoDesc();

    List<Ticket> findByPedidoId(Long pedidoId);
}


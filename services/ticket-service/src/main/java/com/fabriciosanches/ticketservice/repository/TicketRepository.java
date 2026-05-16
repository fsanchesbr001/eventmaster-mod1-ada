package com.fabriciosanches.ticketservice.repository;

import com.fabriciosanches.ticketservice.domain.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    boolean existsByIdIngresso(Long idIngresso);

    boolean existsByIdIngressoAndIdNot(Long idIngresso, Long id);
}


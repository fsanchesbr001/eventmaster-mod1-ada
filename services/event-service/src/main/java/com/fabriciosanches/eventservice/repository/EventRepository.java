package com.fabriciosanches.eventservice.repository;

import com.fabriciosanches.eventservice.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {
}


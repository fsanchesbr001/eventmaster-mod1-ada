package com.fabriciosanches.shared.events;

public record EventCreatedEvent(
        Long eventId,
        String nome,
        Integer capacidade
) {
}


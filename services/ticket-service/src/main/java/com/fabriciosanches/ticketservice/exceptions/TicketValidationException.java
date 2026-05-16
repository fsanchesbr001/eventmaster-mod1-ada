package com.fabriciosanches.ticketservice.exceptions;

public class TicketValidationException extends RuntimeException {
    public TicketValidationException(String message) {
        super(message);
    }
}


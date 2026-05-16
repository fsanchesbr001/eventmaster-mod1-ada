package com.fabriciosanches.eventservice.exceptions;

public class EventValidationException extends RuntimeException {
    public EventValidationException(String message) {
        super(message);
    }
}


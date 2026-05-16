package com.fabriciosanches.orderservice.exceptions;

public class OrderIntegrationException extends RuntimeException {
    public OrderIntegrationException(String message) {
        super(message);
    }
}


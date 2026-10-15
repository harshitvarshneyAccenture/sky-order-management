package com.sky.ordermanagement.order.exception;

/** Business-rule violation on an order (maps to HTTP 400). */
public class InvalidOrderException extends RuntimeException {
    public InvalidOrderException(String message) {
        super(message);
    }
}

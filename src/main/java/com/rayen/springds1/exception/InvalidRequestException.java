package com.rayen.springds1.exception;

/** The request is well formed but its values are not acceptable (HTTP 400). */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}

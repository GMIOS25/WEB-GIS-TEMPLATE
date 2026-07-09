package com.website.gis.exception;

/**
 * Thrown when a request is well-formed but violates a business rule
 * (e.g. duplicate username, deleting one's own account).
 * Handled by {@link GlobalExceptionHandler} and mapped to HTTP 400 Bad Request.
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
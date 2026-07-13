package com.website.gis.core.exception;

/**
 * Thrown when a requested resource (ward, user, etc.) does not exist.
 * Handled by {@link GlobalExceptionHandler} and mapped to HTTP 404 Not Found.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
package com.nivya.common.exception;

/**
 * Exception thrown when a requested domain entity is not found.
 */
public class ResourceNotFoundException extends NivyaException {

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}

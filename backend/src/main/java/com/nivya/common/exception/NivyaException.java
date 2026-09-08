package com.nivya.common.exception;

/**
 * Base unchecked exception for all domain exceptions in Nivya.
 */
public class NivyaException extends RuntimeException {

    public NivyaException(String message) {
        super(message);
    }

    public NivyaException(String message, Throwable cause) {
        super(message, cause);
    }
}

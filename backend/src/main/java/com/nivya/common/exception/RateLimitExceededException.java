package com.nivya.common.exception;

/**
 * Exception thrown when an operation exceeds rate limits or brute-force thresholds.
 */
public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(String message) {
        super(message);
    }
}

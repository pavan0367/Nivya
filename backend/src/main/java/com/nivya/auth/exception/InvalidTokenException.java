package com.nivya.auth.exception;

import com.nivya.common.exception.NivyaException;

/**
 * Thrown when a refresh token is invalid, expired, or revoked.
 */
public class InvalidTokenException extends NivyaException {

    public InvalidTokenException(String message) {
        super(message);
    }
}

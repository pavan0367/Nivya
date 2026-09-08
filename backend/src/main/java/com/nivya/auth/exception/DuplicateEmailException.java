package com.nivya.auth.exception;

import com.nivya.common.exception.NivyaException;

/**
 * Thrown when an account registration attempts to use an already registered email.
 */
public class DuplicateEmailException extends NivyaException {

    public DuplicateEmailException(String email) {
        super("An account with email '" + email + "' is already registered");
    }
}

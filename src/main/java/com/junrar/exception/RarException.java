package com.junrar.exception;

@SuppressWarnings("serial")
public class RarException extends Exception {
    public RarException(Throwable cause) {
        super(cause);
    }

    public RarException() {
    }

    public RarException(String message) {
        super(message);
    }
}

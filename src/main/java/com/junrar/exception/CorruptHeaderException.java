package com.junrar.exception;

@SuppressWarnings("serial")
public class CorruptHeaderException extends RarException {
    public CorruptHeaderException(Throwable cause) {
        super(cause);
    }

    public CorruptHeaderException() {
    }

    public CorruptHeaderException(String message) {
        super(message);
    }
}

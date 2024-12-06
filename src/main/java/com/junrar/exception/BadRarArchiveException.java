package com.junrar.exception;

@SuppressWarnings("serial")
public class BadRarArchiveException extends RarException {
    public BadRarArchiveException(Throwable cause) {
        super(cause);
    }

    public BadRarArchiveException() {
    }
}

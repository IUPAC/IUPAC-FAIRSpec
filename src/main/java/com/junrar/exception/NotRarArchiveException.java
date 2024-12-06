package com.junrar.exception;

@SuppressWarnings("serial")
public class NotRarArchiveException extends RarException {
    public NotRarArchiveException(Throwable cause) {
        super(cause);
    }

    public NotRarArchiveException() {
    }
}

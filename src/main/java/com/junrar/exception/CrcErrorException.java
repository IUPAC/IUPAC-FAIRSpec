package com.junrar.exception;

@SuppressWarnings("serial")
public class CrcErrorException extends RarException {
    public CrcErrorException(Throwable cause) {
        super(cause);
    }

    public CrcErrorException() {
        super();
    }
}

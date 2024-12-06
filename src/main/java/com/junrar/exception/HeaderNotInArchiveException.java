package com.junrar.exception;

@SuppressWarnings("serial")
public class HeaderNotInArchiveException extends RarException {
    public HeaderNotInArchiveException(Throwable cause) {
        super(cause);
    }

    public HeaderNotInArchiveException() {
    }
}

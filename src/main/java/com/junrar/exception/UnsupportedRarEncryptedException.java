package com.junrar.exception;

@SuppressWarnings("serial")
public class UnsupportedRarEncryptedException extends RarException {
    public UnsupportedRarEncryptedException(Throwable cause) {
        super(cause);
    }

    public UnsupportedRarEncryptedException() {
    }
}

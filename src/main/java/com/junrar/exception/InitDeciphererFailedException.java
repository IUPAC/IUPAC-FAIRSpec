package com.junrar.exception;

@SuppressWarnings("serial")
public class InitDeciphererFailedException extends RarException {
    public InitDeciphererFailedException(Throwable cause) {
        super(cause);
    }

    public InitDeciphererFailedException() {
        super();
    }
}

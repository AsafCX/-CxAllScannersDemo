package com.checkmarx.controller.exception;

public class ScmException extends RuntimeException {

    public ScmException(String message) {
        super(message);
    }

    public ScmException(String message, Throwable cause) {
        super(message, cause);
    }
}

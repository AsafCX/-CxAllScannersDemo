package com.checkmarx.controller.exception;

import org.springframework.web.client.HttpClientErrorException;

public class DataStoreException extends RuntimeException {

    public DataStoreException(String message) {
        super(message);
    }

    public DataStoreException(String message, HttpClientErrorException cause) {
        super(message, cause);
    }
}

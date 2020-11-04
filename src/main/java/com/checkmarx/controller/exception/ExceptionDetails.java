package com.checkmarx.controller.exception;

import lombok.Data;
import org.springframework.http.HttpStatus;

import java.time.ZonedDateTime;

@Data
public class ExceptionDetails {

    private final String message;
    private final HttpStatus httpStatus;
    private final ZonedDateTime timestamp;
}

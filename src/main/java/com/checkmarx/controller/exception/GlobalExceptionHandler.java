package com.checkmarx.controller.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = {DataStoreException.class, GitHubException.class})
    public ResponseEntity<Object> handleApiRequestException(DataStoreException e){
            ExceptionDetails exceptionDetails = new ExceptionDetails(
                    e.getMessage(),
                    HttpStatus.EXPECTATION_FAILED,
                    ZonedDateTime.now(ZoneId.of("Z"))
            );
            return new ResponseEntity<>(exceptionDetails, HttpStatus.BAD_REQUEST);
    }

}

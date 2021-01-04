package com.checkmarx.controller.exception;

import com.checkmarx.utils.RestWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.Set;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(value = {ScmException.class})
    public ResponseEntity<Object> handleCustomException(RuntimeException e){
        ExceptionDetails exceptionDetails = ExceptionDetails.builder()
                .message(e.getMessage())
                .localDateTime(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(exceptionDetails, HttpStatus.EXPECTATION_FAILED);
    }

    @ExceptionHandler(value = {DataStoreException.class})
    public ResponseEntity<Object> handleCustomException(HttpClientErrorException e){
        HttpStatus status = HttpStatus.EXPECTATION_FAILED;
        if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
            status = e.getStatusCode();
        }
        ExceptionDetails exceptionDetails = ExceptionDetails.builder()
                .message(e.getMessage())
                .localDateTime(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(exceptionDetails, status);
    }

    @ExceptionHandler(value = {NoSuchBeanDefinitionException.class})
    public ResponseEntity<Object> handleNoSuchBeanDefinitionException(
            NoSuchBeanDefinitionException e){
        log.error("The given Scm isn't supported");
        ExceptionDetails exceptionDetails = ExceptionDetails.builder()
                .message(RestWrapper.SCM_NOT_SUPPORTED)
                .localDateTime(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(exceptionDetails, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(value = { ConstraintViolationException.class })
    public ResponseEntity<Object> handleConstraintViolationException(ConstraintViolationException e) {
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append("Constraint violation:");
        for (ConstraintViolation<?> violation : violations ) {
            strBuilder.append(" ").append(violation.getPropertyPath());
        }
        log.error("Constrain violation: {}", e.getMessage());
        ExceptionDetails exceptionDetails = ExceptionDetails.builder()
                .message(strBuilder.toString())
                .localDateTime(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(exceptionDetails, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler()
    public ResponseEntity<Object> handleRuntimeRequestException(RuntimeException e){
        log.error("Runtime Exception: ", e);
        ExceptionDetails exceptionDetails = ExceptionDetails.builder()
                .message(RestWrapper.GENERAL_RUNTIME_EXCEPTION)
                .localDateTime(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(exceptionDetails, HttpStatus.EXPECTATION_FAILED);
    }

}

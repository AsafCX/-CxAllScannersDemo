package com.checkmarx.controller.exception;

import com.checkmarx.utils.RestHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import java.time.LocalDateTime;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(value = {DataStoreException.class, GitHubException.class})
    public ResponseEntity<Object> handleCustomException(RuntimeException e){
            ExceptionDetails exceptionDetails = ExceptionDetails.builder()
                    .message(e.getMessage())
                    .localDateTime(LocalDateTime.now())
                    .build();
            return new ResponseEntity<>(exceptionDetails, HttpStatus.EXPECTATION_FAILED);
    }

    @ExceptionHandler()
    public ResponseEntity<Object> handleRuntimeRequestException(RuntimeException e){
        log.error("Runtime Exception: ", e);
        ExceptionDetails exceptionDetails = ExceptionDetails.builder()
                .message(RestHelper.GENERAL_RUNTIME_EXCEPTION)
                .localDateTime(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(exceptionDetails, HttpStatus.EXPECTATION_FAILED);
    }

}

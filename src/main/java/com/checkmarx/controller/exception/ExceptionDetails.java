package com.checkmarx.controller.exception;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ExceptionDetails {

    private final String message;
    private final LocalDateTime localDateTime;
}

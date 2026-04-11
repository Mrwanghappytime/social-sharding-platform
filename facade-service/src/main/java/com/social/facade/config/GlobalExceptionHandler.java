package com.social.facade.config;

import com.social.common.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("Unhandled exception: {}, stack trace:", e.getMessage(), e);
        return Result.error(500, "Internal server error: " + e.getMessage());
    }
}

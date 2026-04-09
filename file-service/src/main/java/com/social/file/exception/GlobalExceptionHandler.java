package com.social.file.exception;

import com.social.common.dto.Result;
import com.social.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
        int code = e.getCode();
        if (code == 0) {
            code = 400;
        }
        return ResponseEntity.status(code).body(Result.error(code, e.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<Void>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.error(5002, "File too large. Maximum upload size is 50MB"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleGenericException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(500, "Internal server error: " + e.getMessage()));
    }
}

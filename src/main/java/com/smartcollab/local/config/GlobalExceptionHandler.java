package com.smartcollab.local.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<String> handleBusinessException(RuntimeException e) {
        // IllegalStateException 또는 IllegalArgumentException이 발생하면,
        // HTTP 상태 코드 400 (Bad Request)와 함께 예외 메시지를 클라이언트에게 반환합니다.
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }
}
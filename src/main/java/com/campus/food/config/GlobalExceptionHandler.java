package com.campus.food.config;

import com.campus.food.service.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ErrorBody(LocalDateTime timestamp, int status, String error, String message) {
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorBody> handleBusiness(BusinessException e) {
        return ResponseEntity.status(e.getStatus())
                .body(new ErrorBody(LocalDateTime.now(), e.getStatus().value(),
                        e.getStatus().getReasonPhrase(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorBody> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getField)
                .map(f -> f + " " + e.getBindingResult().getFieldErrors().stream()
                        .filter(fe -> fe.getField().equals(f)).findFirst()
                        .map(FieldError::getDefaultMessage).orElse(""))
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest()
                .body(new ErrorBody(LocalDateTime.now(), 400, "Bad Request", msg));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorBody> handleDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorBody(LocalDateTime.now(), 403, "Forbidden", "没有权限执行该操作"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorBody> handleOther(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorBody(LocalDateTime.now(), 500, "Internal Server Error", e.getMessage()));
    }
}

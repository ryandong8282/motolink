package com.motolink.api.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ApiError> handleDomain(DomainException exception, HttpServletRequest request) {
        return ResponseEntity.status(exception.getStatus()).body(
                ApiError.of(exception.getStatus(), exception.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("请求参数不合法");
        return ResponseEntity.badRequest().body(
                ApiError.of(HttpStatus.BAD_REQUEST, message, request.getRequestURI()));
    }


    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiError> handleConstraint(
            ConstraintViolationException exception,
            HttpServletRequest request) {
        return ResponseEntity.badRequest().body(
                ApiError.of(HttpStatus.BAD_REQUEST, exception.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
        return ResponseEntity.internalServerError().body(
                ApiError.of(HttpStatus.INTERNAL_SERVER_ERROR, "服务内部错误", request.getRequestURI()));
    }

    public record ApiError(
            Instant timestamp,
            int status,
            String error,
            String message,
            String path) {
        static ApiError of(HttpStatus status, String message, String path) {
            return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, path);
        }
    }
}

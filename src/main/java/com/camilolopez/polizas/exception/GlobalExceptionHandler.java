package com.camilolopez.polizas.exception;

import com.camilolopez.polizas.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private ResponseEntity<ApiError> error(HttpStatus status, String code, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(ApiError.of(status.value(), code, message, request.getRequestURI()));
    }
    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ApiError> missing(NotFoundException ex, HttpServletRequest req) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), req);
    }
    @ExceptionHandler(BusinessRuleException.class)
    ResponseEntity<ApiError> conflict(BusinessRuleException ex, HttpServletRequest req) {
        return error(HttpStatus.CONFLICT, "BUSINESS_RULE_VIOLATION", ex.getMessage(), req);
    }
    @ExceptionHandler(CoreIntegrationException.class)
    ResponseEntity<ApiError> core(CoreIntegrationException ex, HttpServletRequest req) {
        return error(HttpStatus.BAD_GATEWAY, "CORE_UNAVAILABLE", "No se pudo actualizar CORE", req);
    }
    @ExceptionHandler(CannotAcquireLockException.class)
    ResponseEntity<ApiError> concurrent(CannotAcquireLockException ex, HttpServletRequest req) {
        return error(HttpStatus.CONFLICT, "CONCURRENT_OPERATION", "Operación concurrente; intente de nuevo", req);
    }
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class, ConstraintViolationException.class,
            IllegalArgumentException.class})
    ResponseEntity<ApiError> invalid(Exception ex, HttpServletRequest req) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Solicitud inválida", req);
    }
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception ex, HttpServletRequest req) {
        log.error("Unexpected API failure path={}", req.getRequestURI(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Error interno", req);
    }
}

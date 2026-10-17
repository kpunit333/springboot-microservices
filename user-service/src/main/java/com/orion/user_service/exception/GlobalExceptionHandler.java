package com.orion.user_service.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.orion.user_service.dto.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument / business validation error: {}", ex.getMessage());
        String msg = ex.getMessage();
        HttpStatus status = HttpStatus.BAD_REQUEST;

        if (msg != null) {
            String lower = msg.toLowerCase();
            if (lower.contains("already exists") || lower.contains("already in use")) {
                status = HttpStatus.CONFLICT;
            } else if (lower.contains("not found")) {
                status = HttpStatus.NOT_FOUND;
            } else if (lower.contains("unauthorized") || lower.contains("invalid credentials")) {
                status = HttpStatus.UNAUTHORIZED;
            } else if (lower.contains("forbidden") || lower.contains("locked") || lower.contains("disabled")) {
                status = HttpStatus.FORBIDDEN;
            }
        }
        return ResponseEntity.status(status).body(ApiResponse.error(msg));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        StringBuilder sb = new StringBuilder("Validation failed: ");
        boolean first = true;
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            if (!first) sb.append(", ");
            sb.append(error.getField()).append(" - ").append(error.getDefaultMessage());
            first = false;
        }
        String errorMsg = sb.toString();
        log.warn("Validation error: {}", errorMsg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(errorMsg));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        String uri = request != null ? request.getRequestURI() : "";
        log.warn("Malformed or unreadable request body for {}: {}", uri, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Malformed request payload: please verify the JSON format and field types"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        String uri = request != null ? request.getRequestURI() : "";
        log.warn("Data integrity violation at {}: {}", uri, ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("Database constraint violation: duplicate record or conflicting data"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        String uri = request != null ? request.getRequestURI() : "";
        log.warn("Unsupported media type at {}: {}", uri, ex.getContentType());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiResponse.error("Unsupported media type: please provide 'application/json'"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        String uri = request != null ? request.getRequestURI() : "";
        log.warn("HTTP method not supported: {} for {}", ex.getMethod(), uri);
        String message = "Request method '" + ex.getMethod() + "' is not supported for " + uri
                + ". Supported method(s): " + ex.getSupportedHttpMethods();
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(ApiResponse.error(message));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(
            NoResourceFoundException ex, HttpServletRequest request) {
        String uri = request != null ? request.getRequestURI() : "/" + ex.getResourcePath();
        String method = ex.getHttpMethod() != null ? ex.getHttpMethod().name() : "REQUEST";
        log.warn("Route not found: {} {}", method, uri);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("No endpoint found for: " + method + " " + uri));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
        log.error("Unhandled server error in user-service: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Internal server error: An unexpected error occurred. Please try again later."));
    }
}

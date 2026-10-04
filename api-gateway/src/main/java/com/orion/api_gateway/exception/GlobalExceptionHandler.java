package com.orion.api_gateway.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.orion.api_gateway.dto.response.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCredentials(InvalidCredentialsException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidToken(InvalidTokenException ex) {
        log.warn("Token validation failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(AccountStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountStatus(AccountStatusException ex) {
        log.warn("Account status rejected: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        StringBuilder sb = new StringBuilder("Validation failed: ");
        boolean first = true;
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(error.getField()).append(" - ").append(error.getDefaultMessage());
            first = false;
        }
        String errorMsg = sb.toString();
        log.warn("Validation error: {}", errorMsg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(errorMsg));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument or entity conflict: {}", ex.getMessage());
        HttpStatus status = ex.getMessage() != null && ex.getMessage().toLowerCase().contains("already exists")
                ? HttpStatus.CONFLICT
                : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handles HTTP method mismatches (e.g. GET instead of POST on login endpoints).
     * Returns 405 Method Not Allowed with tailored validation message for both individual and organization login.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        String uri = request != null ? request.getRequestURI() : "";
        String method = ex.getMethod();
        log.warn("HTTP method not supported: {} for {}", method, uri);

        String message;
        if ("/api/auth/login".equalsIgnoreCase(uri)) {
            message = "Request method '" + method + "' is not supported for individual login. Please use POST.";
        } else if ("/api/auth/o/login".equalsIgnoreCase(uri)) {
            message = "Request method '" + method + "' is not supported for organization login. Please use POST.";
        } else if ("/api/auth/refresh".equalsIgnoreCase(uri)) {
            message = "Request method '" + method + "' is not supported for token refresh. Please use POST.";
        } else if ("/api/auth/logout".equalsIgnoreCase(uri)) {
            message = "Request method '" + method + "' is not supported for logout. Please use POST.";
        } else {
            message = "Request method '" + method + "' is not supported for " + uri
                    + ". Supported method(s): " + ex.getSupportedHttpMethods();
        }

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error(message));
    }

    /**
     * Handles missing or invalid request body (e.g. empty payload, malformed JSON).
     * Returns 400 Bad Request with proper validation message for login requests.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        String uri = request != null ? request.getRequestURI() : "";
        log.warn("Malformed or missing request body for {}: {}", uri, ex.getMessage());

        String message;
        if ("/api/auth/login".equalsIgnoreCase(uri)) {
            message = "Request body is required for individual login. Please provide valid JSON with 'username' and 'password'.";
        } else if ("/api/auth/o/login".equalsIgnoreCase(uri)) {
            message = "Request body is required for organization login. Please provide valid JSON with 'code' and 'password'.";
        } else {
            message = "Required request body is missing or unreadable.";
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    /**
     * Handles unsupported Content-Type header (e.g. text/plain instead of application/json).
     * Returns 415 Unsupported Media Type.
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        String uri = request != null ? request.getRequestURI() : "";
        log.warn("Unsupported media type for {}: {}", uri, ex.getContentType());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiResponse.error("Content-Type '" + ex.getContentType() + "' is not supported. Please use 'application/json'."));
    }

    /**
     * Thrown by Spring MVC when no handler/resource is found for the requested path.
     * Returns 404 Not Found with descriptive validation message for individual/org login paths.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(
            NoResourceFoundException ex, HttpServletRequest request) {
        String uri = request != null ? request.getRequestURI() : "/" + ex.getResourcePath();
        String method = ex.getHttpMethod() != null ? ex.getHttpMethod().name()
                : (request != null ? request.getMethod() : "REQUEST");
        log.warn("Route not found: {} {}", method, uri);

        String message = buildPathNotFoundMessage(uri, method);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(message));
    }

    /**
     * Thrown when DispatcherServlet finds no handler for a request.
     * Returns 404 Not Found with descriptive validation message for individual/org login paths.
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoHandlerFound(
            NoHandlerFoundException ex, HttpServletRequest request) {
        String uri = request != null ? request.getRequestURI() : ex.getRequestURL();
        String method = ex.getHttpMethod();
        log.warn("Handler not found: {} {}", method, uri);

        String message = buildPathNotFoundMessage(uri, method);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(message));
    }

    private String buildPathNotFoundMessage(String uri, String method) {
        String lower = uri.toLowerCase();
        if (lower.contains("/auth") || lower.contains("/login")) {
            if (lower.contains("/o/") || lower.contains("org")) {
                return "Invalid path '" + uri + "' for organization login. The correct endpoint is: POST /api/auth/o/login";
            } else if (lower.contains("user") || lower.contains("individual")) {
                return "Invalid path '" + uri + "' for individual login. The correct endpoint is: POST /api/auth/login";
            } else {
                return "Invalid login path '" + uri + "'. For individual login use POST /api/auth/login, and for organization login use POST /api/auth/o/login";
            }
        }
        return "No endpoint found for: " + method + " " + uri;
    }

    /**
     * Thrown when the load-balancer cannot find a running instance of a downstream service.
     * Returns 503 Service Unavailable instead of 500.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex) {
        String msg = ex.getMessage();
        if (msg != null && msg.contains("No instances available")) {
            log.error("Downstream service unavailable: {}", msg);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error("Downstream service is currently unavailable. Please try again later."));
        }
        log.error("Illegal state: {}", msg);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Internal server error: " + (msg != null ? msg : "An unexpected error occurred.")));
    }

    /**
     * General catch-all for server errors.
     * Returns 500 Internal Server Error with structured response { data: null, message, success: false, timestamp }.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
        log.error("Unhandled server error in API Gateway: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Internal server error: An unexpected error occurred. Please try again later."));
    }
}

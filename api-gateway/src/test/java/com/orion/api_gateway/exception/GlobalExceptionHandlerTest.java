package com.orion.api_gateway.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.orion.api_gateway.dto.response.ApiResponse;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // METHOD NOT ALLOWED (405)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Method mismatch on individual login returns 405 with specific message")
    void handleMethodNotSupported_individualLogin() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/login");
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("GET");

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleMethodNotSupported(ex, request);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Request method 'GET' is not supported for individual login. Please use POST.", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Method mismatch on organization login returns 405 with specific message")
    void handleMethodNotSupported_organizationLogin() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/o/login");
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("GET");

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleMethodNotSupported(ex, request);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Request method 'GET' is not supported for organization login. Please use POST.", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Method mismatch on token refresh returns 405 with specific message")
    void handleMethodNotSupported_tokenRefresh() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/refresh");
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("GET");

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleMethodNotSupported(ex, request);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Request method 'GET' is not supported for token refresh. Please use POST.", response.getBody().getMessage());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PATH NOT FOUND (404)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Path mismatch for individual login returns 404 with guidance")
    void handleNoResourceFound_individualLoginPath() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/user/login");
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.POST, "api/auth/user/login", "No static resource found");

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleNoResourceFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Invalid path '/api/auth/user/login' for individual login. The correct endpoint is: POST /api/auth/login", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Path mismatch for organization login returns 404 with guidance")
    void handleNoResourceFound_organizationLoginPath() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/org/login");
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.POST, "api/auth/org/login", "No static resource found");

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleNoResourceFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Invalid path '/api/auth/org/login' for organization login. The correct endpoint is: POST /api/auth/o/login", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Path mismatch on general auth path returns 404 with both endpoints explained")
    void handleNoResourceFound_generalAuthPath() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/logins");
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.POST, "api/auth/logins", "No static resource found");

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleNoResourceFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Invalid login path '/api/auth/logins'. For individual login use POST /api/auth/login, and for organization login use POST /api/auth/o/login", response.getBody().getMessage());
    }

    @Test
    @DisplayName("NoHandlerFoundException for organization login returns 404 with guidance")
    void handleNoHandlerFound_orgLogin() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/organization/login");
        NoHandlerFoundException ex = new NoHandlerFoundException("POST", "/api/auth/organization/login", null);

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleNoHandlerFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid path '/api/auth/organization/login' for organization login. The correct endpoint is: POST /api/auth/o/login", response.getBody().getMessage());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BODY & MEDIA TYPE (400, 415)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Missing request body on individual login returns 400 with guidance")
    void handleHttpMessageNotReadable_individual() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("Required request body is missing", (HttpInputMessage) null);

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleHttpMessageNotReadable(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Request body is required for individual login. Please provide valid JSON with 'username' and 'password'.", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Missing request body on organization login returns 400 with guidance")
    void handleHttpMessageNotReadable_organization() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/o/login");
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("Required request body is missing", (HttpInputMessage) null);

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleHttpMessageNotReadable(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Request body is required for organization login. Please provide valid JSON with 'code' and 'password'.", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Unsupported media type returns 415")
    void handleMediaTypeNotSupported() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        HttpMediaTypeNotSupportedException ex = new HttpMediaTypeNotSupportedException(MediaType.TEXT_PLAIN, java.util.List.of(MediaType.APPLICATION_JSON));

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleMediaTypeNotSupported(ex, request);

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Content-Type 'text/plain' is not supported. Please use 'application/json'.", response.getBody().getMessage());
    }
}

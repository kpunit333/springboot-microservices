package com.orion.api_gateway.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.orion.api_gateway.dto.request.IndividualLoginRequest;
import com.orion.api_gateway.dto.request.LogoutRequest;
import com.orion.api_gateway.dto.request.OrganizationLoginRequest;
import com.orion.api_gateway.dto.request.RefreshTokenRequest;
import com.orion.api_gateway.dto.response.ApiResponse;
import com.orion.api_gateway.dto.response.AuthResponse;
import com.orion.api_gateway.dto.response.TokenRefreshResponse;
import com.orion.api_gateway.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * Individual / User login.
     * Receives user credentials, validates with user-service, and returns access &
     * refresh tokens.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> loginIndividual(
            @Valid @RequestBody IndividualLoginRequest request) {
        log.info("Received login request for user: {}", request.getUsername());
        AuthResponse response = authService.loginIndividual(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    /**
     * Organization login.
     * Receives org credentials, validates with organization-service, and returns
     * access & refresh tokens.
     */
    @PostMapping("/o/login")
    public ResponseEntity<ApiResponse<AuthResponse>> loginOrganization(
            @Valid @RequestBody OrganizationLoginRequest request) {
        log.info("Received login request for organization code: {}", request.getCode());
        AuthResponse response = authService.loginOrganization(request);
        return ResponseEntity.ok(ApiResponse.ok("Organization login successful", response));
    }

    /**
     * Refresh access and refresh tokens using an existing refresh token.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        log.info("Received token refresh request");
        TokenRefreshResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.ok("Token refreshed successfully", response));
    }

    /**
     * Logout and invalidate current tokens.
     */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @RequestBody(required = false) LogoutRequest request) {
        log.info("Received logout request");
        authService.logout(authHeader, request);
        return ResponseEntity.ok(ApiResponse.ok("Successfully logged out", null));
    }
}

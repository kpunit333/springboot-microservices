package com.orion.api_gateway.service;

import com.orion.api_gateway.dto.request.IndividualLoginRequest;
import com.orion.api_gateway.dto.request.LogoutRequest;
import com.orion.api_gateway.dto.request.OrganizationLoginRequest;
import com.orion.api_gateway.dto.request.RefreshTokenRequest;
import com.orion.api_gateway.dto.response.AuthResponse;
import com.orion.api_gateway.dto.response.TokenRefreshResponse;

public interface AuthService {

    /**
     * Authenticate an individual user by querying user-service and validating credentials.
     */
    AuthResponse loginIndividual(IndividualLoginRequest request);

    /**
     * Authenticate an organization by querying organization-service and validating credentials.
     */
    AuthResponse loginOrganization(OrganizationLoginRequest request);

    /**
     * Refresh tokens using a valid refresh token.
     */
    TokenRefreshResponse refreshToken(RefreshTokenRequest request);

    /**
     * Logout and invalidate access / refresh tokens.
     */
    void logout(String authHeader, LogoutRequest request);
}

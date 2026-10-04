package com.orion.api_gateway.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.orion.api_gateway.client.OrganizationServiceClient;
import com.orion.api_gateway.client.UserServiceClient;
import com.orion.api_gateway.client.dto.OrgInternalDto;
import com.orion.api_gateway.client.dto.UserInternalDto;
import com.orion.api_gateway.config.SecurityConfig;
import com.orion.api_gateway.dto.request.IndividualLoginRequest;
import com.orion.api_gateway.dto.request.LogoutRequest;
import com.orion.api_gateway.dto.request.OrganizationLoginRequest;
import com.orion.api_gateway.dto.request.RefreshTokenRequest;
import com.orion.api_gateway.dto.response.AuthResponse;
import com.orion.api_gateway.dto.response.TokenRefreshResponse;
import com.orion.api_gateway.exception.AccountStatusException;
import com.orion.api_gateway.exception.InvalidCredentialsException;
import com.orion.api_gateway.service.impl.AuthServiceImpl;

import io.jsonwebtoken.Claims;

class AuthServiceTest {

    private UserServiceClient userServiceClient;
    private OrganizationServiceClient organizationServiceClient;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private TokenBlacklistService tokenBlacklistService;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        userServiceClient = mock(UserServiceClient.class);
        organizationServiceClient = mock(OrganizationServiceClient.class);
        passwordEncoder = new SecurityConfig().passwordEncoder();
        jwtService = mock(JwtService.class);
        tokenBlacklistService = mock(TokenBlacklistService.class);

        authService = new AuthServiceImpl(
                userServiceClient,
                organizationServiceClient,
                passwordEncoder,
                jwtService,
                tokenBlacklistService);
    }

    @Test
    void testIndividualLoginSuccess() {
        UserInternalDto user = UserInternalDto.builder()
                .userId(1L)
                .username("puneet")
                .name("Puneet Shaw")
                .password("plain_pass_123")
                .status("ACTIVE")
                .role("USER")
                .build();

        when(userServiceClient.getUserByUsername("puneet")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(eq("1"), anyMap())).thenReturn("access_token_123");
        when(jwtService.generateRefreshToken(eq("1"), anyMap())).thenReturn("refresh_token_123");
        when(jwtService.getAccessTokenExpirationMillis()).thenReturn(900000L);

        IndividualLoginRequest request = new IndividualLoginRequest("puneet", "plain_pass_123");
        AuthResponse response = authService.loginIndividual(request);

        assertNotNull(response);
        assertEquals("access_token_123", response.getAccessToken());
        assertEquals("refresh_token_123", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
    }

    @Test
    void testIndividualLoginInvalidCredentials() {
        UserInternalDto user = UserInternalDto.builder()
                .userId(1L)
                .username("puneet")
                .password("plain_pass_123")
                .status("ACTIVE")
                .build();

        when(userServiceClient.getUserByUsername("puneet")).thenReturn(Optional.of(user));

        IndividualLoginRequest request = new IndividualLoginRequest("puneet", "wrong_pass");
        assertThrows(InvalidCredentialsException.class, () -> {
            authService.loginIndividual(request);
        });
    }

    @Test
    void testIndividualLoginInactiveAccount() {
        UserInternalDto user = UserInternalDto.builder()
                .userId(1L)
                .username("puneet")
                .password("plain_pass_123")
                .status("LOCKED")
                .build();

        when(userServiceClient.getUserByUsername("puneet")).thenReturn(Optional.of(user));

        IndividualLoginRequest request = new IndividualLoginRequest("puneet", "plain_pass_123");
        AccountStatusException ex = assertThrows(AccountStatusException.class, () -> {
            authService.loginIndividual(request);
        });
        assertTrue(ex.getMessage().contains("LOCKED"));
    }

    @Test
    void testOrganizationLoginSuccess() {
        OrgInternalDto org = OrgInternalDto.builder()
                .id(10L)
                .name("Accenture")
                .code("ORG1001")
                .password("org_pass")
                .status("ACTIVE")
                .build();

        when(organizationServiceClient.getOrganizationByCode("ORG1001")).thenReturn(Optional.of(org));
        when(jwtService.generateAccessToken(eq("10"), anyMap())).thenReturn("org_access_token");
        when(jwtService.generateRefreshToken(eq("10"), anyMap())).thenReturn("org_refresh_token");
        when(jwtService.getAccessTokenExpirationMillis()).thenReturn(900000L);

        OrganizationLoginRequest request = new OrganizationLoginRequest("ORG1001", "org_pass");
        AuthResponse response = authService.loginOrganization(request);

        assertNotNull(response);
        assertEquals("org_access_token", response.getAccessToken());
        assertEquals("org_refresh_token", response.getRefreshToken());
    }

    @Test
    void testOrganizationLoginSuccessByNameOnly() {
        OrgInternalDto org = OrgInternalDto.builder()
                .id(10L)
                .name("Accenture")
                .code("ORG1001")
                .password("org_pass")
                .status("ACTIVE")
                .build();

        when(organizationServiceClient.getOrganizationByName("Accenture")).thenReturn(Optional.of(org));
        when(jwtService.generateAccessToken(eq("10"), anyMap())).thenReturn("org_access_token");
        when(jwtService.generateRefreshToken(eq("10"), anyMap())).thenReturn("org_refresh_token");
        when(jwtService.getAccessTokenExpirationMillis()).thenReturn(900000L);

        OrganizationLoginRequest request = OrganizationLoginRequest.builder()
                .name("Accenture")
                .password("org_pass")
                .build();
        AuthResponse response = authService.loginOrganization(request);

        assertNotNull(response);
        assertEquals("org_access_token", response.getAccessToken());
        assertEquals("org_refresh_token", response.getRefreshToken());
    }

    @Test
    void testOrganizationLoginSuccessWithBothCodeAndName_FallbackToName() {
        OrgInternalDto org = OrgInternalDto.builder()
                .id(10L)
                .name("Accenture")
                .code("ORG1001")
                .password("org_pass")
                .status("ACTIVE")
                .build();

        when(organizationServiceClient.getOrganizationByCode("WRONG_CODE")).thenReturn(Optional.empty());
        when(organizationServiceClient.getOrganizationByName("Accenture")).thenReturn(Optional.of(org));
        when(jwtService.generateAccessToken(eq("10"), anyMap())).thenReturn("org_access_token");
        when(jwtService.generateRefreshToken(eq("10"), anyMap())).thenReturn("org_refresh_token");
        when(jwtService.getAccessTokenExpirationMillis()).thenReturn(900000L);

        OrganizationLoginRequest request = OrganizationLoginRequest.builder()
                .code("WRONG_CODE")
                .name("Accenture")
                .password("org_pass")
                .build();
        AuthResponse response = authService.loginOrganization(request);

        assertNotNull(response);
        assertEquals("org_access_token", response.getAccessToken());
        assertEquals("org_refresh_token", response.getRefreshToken());
    }

    @Test
    void testOrganizationLoginFailureWhenNeitherMatches() {
        when(organizationServiceClient.getOrganizationByCode("WRONG_CODE")).thenReturn(Optional.empty());
        when(organizationServiceClient.getOrganizationByName("WRONG_NAME")).thenReturn(Optional.empty());

        OrganizationLoginRequest request = OrganizationLoginRequest.builder()
                .code("WRONG_CODE")
                .name("WRONG_NAME")
                .password("org_pass")
                .build();

        assertThrows(InvalidCredentialsException.class, () -> authService.loginOrganization(request));
    }

    @Test
    void testRefreshTokenSuccess() {
        Claims mockClaims = mock(Claims.class);
        when(mockClaims.getSubject()).thenReturn("1");
        when(mockClaims.getExpiration()).thenReturn(new java.util.Date(System.currentTimeMillis() + 60000));
        when(jwtService.validateRefreshToken("valid_refresh_token")).thenReturn(mockClaims);
        when(jwtService.generateAccessToken(eq("1"), anyMap())).thenReturn("new_access_token");
        when(jwtService.generateRefreshToken(eq("1"), anyMap())).thenReturn("new_refresh_token");
        when(jwtService.getAccessTokenExpirationMillis()).thenReturn(900000L);

        TokenRefreshResponse response = authService.refreshToken(new RefreshTokenRequest("valid_refresh_token"));

        assertNotNull(response);
        assertEquals("new_access_token", response.getAccessToken());
        assertEquals("new_refresh_token", response.getRefreshToken());
        verify(tokenBlacklistService).blacklistToken(eq("valid_refresh_token"), any());
    }

    @Test
    void testLogoutBlacklistsTokens() {
        Claims mockClaims = mock(Claims.class);
        when(mockClaims.getExpiration()).thenReturn(new java.util.Date(System.currentTimeMillis() + 60000));
        when(jwtService.parseAndValidate(anyString())).thenReturn(mockClaims);

        authService.logout("Bearer access_token_abc", new LogoutRequest("refresh_token_xyz"));

        verify(tokenBlacklistService).blacklistToken(eq("access_token_abc"), any());
        verify(tokenBlacklistService).blacklistToken(eq("refresh_token_xyz"), any());
    }
}

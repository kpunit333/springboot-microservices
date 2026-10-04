package com.orion.api_gateway.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.orion.api_gateway.config.JwtProperties;
import com.orion.api_gateway.exception.InvalidTokenException;

import io.jsonwebtoken.Claims;

class JwtServiceTest {

    private JwtProperties jwtProperties;
    private TokenBlacklistService tokenBlacklistService;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret(
                "dGhpc0lzQVZlcnlTZWN1cmVTZWNyZXRLZXlGb3JKd3RUb2tlbkdlbmVyYXRpb25Jbk9yaW9uQXBwbGljYXRpb24xMjM0NTY3ODkwIQ==");
        jwtProperties.setAccessTokenExpiration(60000); // 1 minute
        jwtProperties.setRefreshTokenExpiration(120000); // 2 minutes

        tokenBlacklistService = new TokenBlacklistService();
        jwtService = new JwtService(jwtProperties, tokenBlacklistService);
        jwtService.init();
    }

    @Test
    void testGenerateAndValidateAccessToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 101L);
        claims.put("username", "john_doe");
        claims.put(JwtService.CLAIM_ROLE, "USER");

        String token = jwtService.generateAccessToken("101", claims);
        assertNotNull(token);

        Claims parsedClaims = jwtService.parseAndValidate(token);
        assertEquals("101", parsedClaims.getSubject());
        assertEquals("john_doe", parsedClaims.get("username"));
        assertEquals("USER", parsedClaims.get(JwtService.CLAIM_ROLE));
        assertEquals(JwtService.TYPE_ACCESS, parsedClaims.get(JwtService.CLAIM_TOKEN_TYPE));
    }

    @Test
    void testRefreshTokenValidation() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 101L);

        String refreshToken = jwtService.generateRefreshToken("101", claims);
        assertNotNull(refreshToken);

        Claims parsedClaims = jwtService.validateRefreshToken(refreshToken);
        assertEquals("101", parsedClaims.getSubject());
        assertEquals(JwtService.TYPE_REFRESH, parsedClaims.get(JwtService.CLAIM_TOKEN_TYPE));
    }

    @Test
    void testValidateRefreshTokenThrowsWhenGivenAccessToken() {
        Map<String, Object> claims = new HashMap<>();
        String accessToken = jwtService.generateAccessToken("101", claims);

        InvalidTokenException ex = assertThrows(InvalidTokenException.class, () -> {
            jwtService.validateRefreshToken(accessToken);
        });
        assertTrue(ex.getMessage().contains("not a refresh token"));
    }

    @Test
    void testBlacklistedTokenIsRejected() {
        Map<String, Object> claims = new HashMap<>();
        String token = jwtService.generateAccessToken("101", claims);

        // Before blacklist
        assertNotNull(jwtService.parseAndValidate(token));

        // Blacklist token
        tokenBlacklistService.blacklistToken(token, Instant.now().plusSeconds(60));

        // After blacklist
        assertThrows(InvalidTokenException.class, () -> {
            jwtService.parseAndValidate(token);
        });
    }
}

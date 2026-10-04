package com.orion.api_gateway.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.orion.api_gateway.config.JwtProperties;
import com.orion.api_gateway.exception.InvalidTokenException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {

    public static final String CLAIM_TOKEN_TYPE = "type";
    public static final String CLAIM_USER_TYPE = "userType";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_NAME = "name";

    public static final String TYPE_ACCESS = "ACCESS";
    public static final String TYPE_REFRESH = "REFRESH";

    private final JwtProperties jwtProperties;
    private final TokenBlacklistService tokenBlacklistService;
    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
        } catch (Exception e) {
            keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        }
        if (keyBytes.length < 32) {
            keyBytes = Arrays.copyOf(keyBytes, 32);
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generate short-lived Access Token.
     */
    public String generateAccessToken(String subject, Map<String, Object> claims) {
        return buildToken(subject, claims, TYPE_ACCESS, jwtProperties.getAccessTokenExpiration());
    }

    /**
     * Generate longer-lived Refresh Token.
     */
    public String generateRefreshToken(String subject, Map<String, Object> claims) {
        return buildToken(subject, claims, TYPE_REFRESH, jwtProperties.getRefreshTokenExpiration());
    }

    private String buildToken(String subject, Map<String, Object> claims, String tokenType, long expirationMs) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expirationMs);

        return Jwts.builder()
                .subject(subject)
                .claims(claims)
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Parse and validate claims from token. Checks expiration and blacklist.
     */
    public Claims parseAndValidate(String token) {
        if (tokenBlacklistService.isBlacklisted(token)) {
            throw new InvalidTokenException("Token has been revoked/logged out");
        }
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new InvalidTokenException("Token has expired", e);
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("Malformed or invalid token: " + e.getMessage(), e);
        }
    }

    /**
     * Validate that the token is specifically a REFRESH token.
     */
    public Claims validateRefreshToken(String token) {
        Claims claims = parseAndValidate(token);
        String tokenType = claims.get(CLAIM_TOKEN_TYPE, String.class);
        if (!TYPE_REFRESH.equals(tokenType)) {
            throw new InvalidTokenException("Provided token is not a refresh token");
        }
        return claims;
    }

    public Instant getExpiration(String token) {
        Claims claims = parseAndValidate(token);
        return claims.getExpiration().toInstant();
    }

    public long getAccessTokenExpirationMillis() {
        return jwtProperties.getAccessTokenExpiration();
    }

    public long getRefreshTokenExpirationMillis() {
        return jwtProperties.getRefreshTokenExpiration();
    }
}

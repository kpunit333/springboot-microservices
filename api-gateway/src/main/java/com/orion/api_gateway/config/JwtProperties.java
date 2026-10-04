package com.orion.api_gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtProperties {

    /**
     * Secret key for signing HMAC SHA-256 JWT tokens.
     */
    private String secret = "dGhpc0lzQVZlcnlTZWN1cmVTZWNyZXRLZXlGb3JKd3RUb2tlbkdlbmVyYXRpb25Jbk9yaW9uQXBwbGljYXRpb24xMjM0NTY3ODkwIQ==";

    /**
     * Access token validity in milliseconds (default 15 minutes = 900000ms).
     */
    private long accessTokenExpiration = 900000L;

    /**
     * Refresh token validity in milliseconds (default 7 days = 604800000ms).
     */
    private long refreshTokenExpiration = 604800000L;
}

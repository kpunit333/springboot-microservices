package com.orion.api_gateway.service.impl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.orion.api_gateway.client.OrganizationServiceClient;
import com.orion.api_gateway.client.UserServiceClient;
import com.orion.api_gateway.client.dto.OrgInternalDto;
import com.orion.api_gateway.client.dto.UserInternalDto;
import com.orion.api_gateway.dto.request.IndividualLoginRequest;
import com.orion.api_gateway.dto.request.LogoutRequest;
import com.orion.api_gateway.dto.request.OrganizationLoginRequest;
import com.orion.api_gateway.dto.request.RefreshTokenRequest;
import com.orion.api_gateway.dto.response.AuthResponse;
import com.orion.api_gateway.dto.response.OrgProfileDto;
import com.orion.api_gateway.dto.response.TokenRefreshResponse;
import com.orion.api_gateway.dto.response.UserProfileDto;
import com.orion.api_gateway.exception.AccountStatusException;
import com.orion.api_gateway.exception.InvalidCredentialsException;
import com.orion.api_gateway.service.AuthService;
import com.orion.api_gateway.service.JwtService;
import com.orion.api_gateway.service.TokenBlacklistService;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserServiceClient userServiceClient;
    private final OrganizationServiceClient organizationServiceClient;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    public AuthResponse loginIndividual(IndividualLoginRequest request) {
        log.info("Processing individual login for username: {}", request.getUsername());

        UserInternalDto user = userServiceClient.getUserByUsername(request.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

        log.info("Processed individual login for username: {}", user);

        // Validate account status
        if (user.getStatus() != null && !"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new AccountStatusException("Account status is " + user.getStatus());
        }

        // Validate expiration
        if (user.getExpireAt() != null && user.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new AccountStatusException("Account has expired");
        }

        // Validate password credentials
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        // Build token claims
        String role = user.getRole() != null ? user.getRole() : "USER";
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getUserId());
        claims.put("username", user.getUsername());
        claims.put(JwtService.CLAIM_NAME, user.getName());
        claims.put(JwtService.CLAIM_ROLE, role);
        claims.put(JwtService.CLAIM_USER_TYPE, "INDIVIDUAL");

        String subject = String.valueOf(user.getUserId());
        String accessToken = jwtService.generateAccessToken(subject, claims);
        String refreshToken = jwtService.generateRefreshToken(subject, claims);

        UserProfileDto profile = UserProfileDto.builder()
                .username(user.getUsername())
                .name(user.getName())
                .role(role)
                .status(user.getStatus())
                .build();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationMillis() / 1000)
                .user(profile)
                .build();
    }

    @Override
    public AuthResponse loginOrganization(OrganizationLoginRequest request) {
        String code = request.getCode() != null ? request.getCode().trim() : null;
        String name = request.getName() != null ? request.getName().trim() : null;

        log.info("Processing organization login for code='{}', name='{}'", code, name);

        if ((code == null || code.isBlank()) && (name == null || name.isBlank())) {
            throw new InvalidCredentialsException("Either organization code or name must be provided");
        }

        OrgInternalDto org = null;

        // 1. If code is provided, check if organization exists and password matches
        if (code != null && !code.isBlank()) {
            Optional<OrgInternalDto> byCode = organizationServiceClient.getOrganizationByCode(code);
            if (byCode.isPresent() && passwordEncoder.matches(request.getPassword(), byCode.get().getPassword())) {
                org = byCode.get();
            }
        }

        // 2. If not matched yet and name is provided, check if organization exists and password matches
        if (org == null && name != null && !name.isBlank()) {
            Optional<OrgInternalDto> byName = organizationServiceClient.getOrganizationByName(name);
            if (byName.isPresent() && passwordEncoder.matches(request.getPassword(), byName.get().getPassword())) {
                org = byName.get();
            }
        }

        // 3. If neither matched with correct password
        if (org == null) {
            log.warn("Organization login failed: no matching organization for code='{}' or name='{}' with valid password", code, name);
            throw new InvalidCredentialsException("Invalid organization code/name or password");
        }

        // Build token claims
        Map<String, Object> claims = new HashMap<>();
        claims.put("orgId", org.getId());
        claims.put("code", org.getCode());
        claims.put(JwtService.CLAIM_NAME, org.getName());
        claims.put(JwtService.CLAIM_ROLE, "ROLE_ORGANIZATION");
        claims.put(JwtService.CLAIM_USER_TYPE, "ORGANIZATION");

        String subject = String.valueOf(org.getId());
        String accessToken = jwtService.generateAccessToken(subject, claims);
        String refreshToken = jwtService.generateRefreshToken(subject, claims);

        OrgProfileDto profile = OrgProfileDto.builder()
                .name(org.getName())
                .code(org.getCode())
                .status(org.getStatus())
                .build();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationMillis() / 1000)
                .user(profile)
                .build();
    }

    @Override
    public TokenRefreshResponse refreshToken(RefreshTokenRequest request) {
        String oldRefreshToken = request.getRefreshToken();
        Claims claims = jwtService.validateRefreshToken(oldRefreshToken);

        String subject = claims.getSubject();
        Map<String, Object> newClaims = new HashMap<>();
        claims.forEach((key, value) -> {
            // Exclude registered standard JWT claims
            if (!Claims.ISSUER.equals(key) &&
                    !Claims.SUBJECT.equals(key) &&
                    !Claims.AUDIENCE.equals(key) &&
                    !Claims.EXPIRATION.equals(key) &&
                    !Claims.NOT_BEFORE.equals(key) &&
                    !Claims.ISSUED_AT.equals(key) &&
                    !Claims.ID.equals(key) &&
                    !JwtService.CLAIM_TOKEN_TYPE.equals(key)) {
                newClaims.put(key, value);
            }
        });

        // Rotate tokens
        String newAccessToken = jwtService.generateAccessToken(subject, newClaims);
        String newRefreshToken = jwtService.generateRefreshToken(subject, newClaims);

        // Invalidate old refresh token
        tokenBlacklistService.blacklistToken(oldRefreshToken, claims.getExpiration().toInstant());

        return TokenRefreshResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationMillis() / 1000)
                .build();
    }

    @Override
    public void logout(String authHeader, LogoutRequest request) {
        // Blacklist access token if present
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            try {
                Claims claims = jwtService.parseAndValidate(token);
                tokenBlacklistService.blacklistToken(token, claims.getExpiration().toInstant());
                log.info("Blacklisted access token during logout");
            } catch (Exception e) {
                log.debug("Access token during logout could not be parsed or already expired: {}", e.getMessage());
            }
        }

        // Blacklist refresh token if present
        if (request != null && request.getRefreshToken() != null && !request.getRefreshToken().isBlank()) {
            String refreshToken = request.getRefreshToken().trim();
            try {
                Claims claims = jwtService.parseAndValidate(refreshToken);
                tokenBlacklistService.blacklistToken(refreshToken, claims.getExpiration().toInstant());
                log.info("Blacklisted refresh token during logout");
            } catch (Exception e) {
                log.debug("Refresh token during logout could not be parsed or already expired: {}", e.getMessage());
            }
        }
    }
}

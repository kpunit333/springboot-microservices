package com.orion.otp_service.service;

import java.security.SecureRandom;

import org.springframework.stereotype.Service;

import com.orion.otp_service.client.OrganizationServiceClient;
import com.orion.otp_service.config.OtpProperties;
import com.orion.otp_service.dto.OtpGenerateRequest;
import com.orion.otp_service.dto.OtpGenerateResponse;
import com.orion.otp_service.dto.OtpVerifyRequest;
import com.orion.otp_service.dto.ValidationResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OtpService {

    private final UpstashRedisService redisService;
    private final OrganizationServiceClient orgClient;
    private final OtpProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpService(
            UpstashRedisService redisService,
            OrganizationServiceClient orgClient,
            OtpProperties properties) {
        this.redisService = redisService;
        this.orgClient = orgClient;
        this.properties = properties;
    }

    /**
     * Generates a 6-digit OTP after validating the secretKey and purposeId against organization-service.
     * Deletes any existing OTP for the same (secretKey, userId) pair.
     * Stores the new OTP in Redis with a 5-minute TTL.
     */
    public OtpGenerateResponse generateOtp(OtpGenerateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body cannot be null");
        }

        String secretKey = sanitize(request.getSecretKey());
        String userId = sanitize(request.getUserId());
        String purposeId = sanitize(request.getPurposeId());

        if (secretKey == null || secretKey.isEmpty()) {
            throw new IllegalArgumentException("Secret key is required");
        }
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (purposeId == null || purposeId.isEmpty()) {
            throw new IllegalArgumentException("Purpose ID is required");
        }

        // 1. Single validation API: check if secretKey and purposeId are valid and from same organization
        ValidationResponse validation = orgClient.validateProjectAndPurpose(secretKey, purposeId);
        if (validation == null || !validation.isValid()) {
            String msg = (validation != null && validation.getMessage() != null)
                    ? validation.getMessage()
                    : "Project and purpose validation failed";
            log.warn("Validation failed for secretKey='{}', purposeId='{}': {}", secretKey, purposeId, msg);
            throw new IllegalArgumentException(msg);
        }

        // 2. Check if OTP from same secretKey and userId is already present; if so, delete it
        String redisKey = buildKey(secretKey, userId);
        if (redisService.exists(redisKey)) {
            log.info("Existing OTP found for key '{}'. Deleting prior to new generation.", redisKey);
            redisService.delete(redisKey);
        }

        // 3. Generate 6-digit OTP
        int number = secureRandom.nextInt(1_000_000);
        String otp = String.format("%06d", number);

        // 4. Store in Redis with TTL 5 minutes (default 300 seconds)
        long ttlSeconds = properties.getTtlSeconds();
        redisService.setWithTtl(redisKey, otp, ttlSeconds);
        log.info("Generated and saved 6-digit OTP for key '{}' with TTL {}s", redisKey, ttlSeconds);

        String effectiveSecretKey = validation.getSecretKey() != null ? validation.getSecretKey() : secretKey;

        return OtpGenerateResponse.builder()
                .secretKey(effectiveSecretKey)
                .userId(userId)
                .purposeId(purposeId)
                .otp(otp)
                .expiresInSeconds(ttlSeconds)
                .build();
    }

    /**
     * Verifies the submitted OTP against Redis for the given secretKey and userId.
     * If correct, deletes the OTP from Redis and completes successfully.
     */
    public void verifyOtp(OtpVerifyRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body cannot be null");
        }

        String secretKey = sanitize(request.getSecretKey());
        String userId = sanitize(request.getUserId());
        String otp = sanitize(request.getOtp());

        if (secretKey == null || secretKey.isEmpty()) {
            throw new IllegalArgumentException("Secret key is required");
        }
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (otp == null || otp.isEmpty()) {
            throw new IllegalArgumentException("OTP is required");
        }

        String redisKey = buildKey(secretKey, userId);
        String storedOtp = redisService.get(redisKey);

        if (storedOtp == null || storedOtp.trim().isEmpty()) {
            log.warn("OTP verification failed — no active OTP found for key '{}'", redisKey);
            throw new IllegalArgumentException("OTP has expired or does not exist");
        }

        if (!storedOtp.trim().equals(otp.trim())) {
            log.warn("OTP verification mismatch for key '{}'", redisKey);
            throw new IllegalArgumentException("Invalid OTP");
        }

        // OTP is correct: delete from Redis
        redisService.delete(redisKey);
        log.info("OTP verified successfully and removed from Redis for key '{}'", redisKey);
    }

    private String buildKey(String secretKey, String userId) {
        return "otp:" + secretKey.trim() + ":" + userId.trim();
    }

    private String sanitize(String value) {
        return value != null ? value.trim() : null;
    }
}

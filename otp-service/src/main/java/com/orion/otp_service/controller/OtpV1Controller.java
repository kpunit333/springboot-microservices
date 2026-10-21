package com.orion.otp_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orion.otp_service.dto.ApiResponse;
import com.orion.otp_service.dto.OtpGenerateRequest;
import com.orion.otp_service.dto.OtpGenerateResponse;
import com.orion.otp_service.dto.OtpVerifyRequest;
import com.orion.otp_service.service.OtpService;

import lombok.extern.slf4j.Slf4j;

/**
 * Controller for OTP generation and verification.
 *
 * Endpoints:
 * POST /api/otp/v1/generate
 * POST /api/otp/v1/verify
 */
@Slf4j
@RestController
@RequestMapping("/api/otp/v1")
public class OtpV1Controller {

    private final OtpService otpService;

    public OtpV1Controller(OtpService otpService) {
        this.otpService = otpService;
    }

    /**
     * POST /api/otp/v1/generate
     *
     * Takes secret key, user id, and purpose id.
     * Validates secret key and purpose id.
     * Deletes any existing OTP for the same (secretKey, userId).
     * Generates a 6-digit OTP and stores it in Upstash Redis with a 5-minute TTL.
     */
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<OtpGenerateResponse>> generateOtp(@RequestBody OtpGenerateRequest request) {
        log.info("Received OTP generation request for secretKey={}, userId={}, purposeId={}",
                request != null ? request.getSecretKey() : null,
                request != null ? request.getUserId() : null,
                request != null ? request.getPurposeId() : null);

        OtpGenerateResponse response = otpService.generateOtp(request);
        log.info("OTP generated successfully for secretKey={}, userId={}", response.getSecretKey(),
                response.getUserId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("OTP generated successfully", response));
    }

    /**
     * POST /api/otp/v1/verify
     *
     * Takes secret key, user id, and otp.
     * Verifies the OTP from Redis.
     * If correct, sends success and deletes OTP from Redis.
     */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(@RequestBody OtpVerifyRequest request) {
        log.info("Received OTP verification request for secretKey={}, userId={}",
                request != null ? request.getSecretKey() : null,
                request != null ? request.getUserId() : null);

        otpService.verifyOtp(request);
        log.info("OTP verified successfully for secretKey={}, userId={}",
                request != null ? request.getSecretKey() : null,
                request != null ? request.getUserId() : null);

        return ResponseEntity.ok(ApiResponse.ok("OTP verified successfully", null));
    }
}

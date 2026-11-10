package com.orion.otp_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
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
 * POST /api/otp/v1/generate and POST /v1/generate
 * POST /api/otp/v1/verify and POST /v1/verify
 */
@Slf4j
@RestController
@RequestMapping({ "/api/otp/v1", "/v1" })
public class OtpV1Controller {

        private final OtpService otpService;

        @Autowired
        public OtpV1Controller(OtpService otpService) {
                this.otpService = otpService;
        }

        /**
         * POST /api/otp/v1/generate or /v1/generate
         *
         * Payload:
         * {
         * "secretKey": "",
         * "purposeId": "",
         * "user": {
         * "id": "",
         * "email": "",
         * "phone": "",
         * "countryCode": "",
         * "whatsapp": ""
         * }
         * }
         */
        @PostMapping("/generate")
        public ResponseEntity<ApiResponse<OtpGenerateResponse>> generateOtp(@RequestBody OtpGenerateRequest request) {
                String userId = request != null && request.getUser() != null ? request.getUser().getId() : null;
                log.info("Received OTP generation request for secretKey={}, userId={}, purposeId={}",
                                request != null ? request.getSecretKey() : null,
                                userId,
                                request != null ? request.getPurposeId() : null);

                OtpGenerateResponse response = otpService.generateOtp(request);
                String respUserId = response.getUser() != null ? response.getUser().getId() : null;
                log.info("OTP generated successfully for secretKey={}, userId={}", response.getSecretKey(),
                                respUserId);

                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.ok("OTP generated successfully", response));
        }

        /**
         * POST /api/otp/v1/verify or /v1/verify
         *
         * Payload:
         * {
         * "secretKey": "",
         * "user": {
         * "id": ""
         * },
         * "otp": ""
         * }
         */
        @PostMapping("/verify")
        public ResponseEntity<ApiResponse<Void>> verifyOtp(@RequestBody OtpVerifyRequest request) {
                String userId = request != null && request.getUser() != null ? request.getUser().getId() : null;
                log.info("Received OTP verification request for secretKey={}, userId={}",
                                request != null ? request.getSecretKey() : null,
                                userId);

                otpService.verifyOtp(request);
                log.info("OTP verified successfully for secretKey={}, userId={}",
                                request != null ? request.getSecretKey() : null,
                                userId);

                return ResponseEntity.ok(ApiResponse.ok("OTP verified successfully", null));
        }
}

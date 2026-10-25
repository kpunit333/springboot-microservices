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
 * Controller for test endpoints mapped directly at root without /api/otp/v1
 * prefix:
 * POST /api/otp/v1/test/generate
 * POST /api/otp/v1/test/verify
 */
@Slf4j
@RestController
@RequestMapping("/api/otp/v1/test")
public class OtpV1TestController {

        private final OtpService otpService;

        public OtpV1TestController(OtpService otpService) {
                this.otpService = otpService;
        }

        @PostMapping("/generate")
        public ResponseEntity<ApiResponse<OtpGenerateResponse>> generateOtp(
                        @RequestBody OtpGenerateRequest request) {
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

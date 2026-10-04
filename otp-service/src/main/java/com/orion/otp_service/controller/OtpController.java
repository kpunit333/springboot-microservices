package com.orion.otp_service.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

/**
 * Main controller for the otp-service, mounted at /api/otp.
 *
 * Future OTP operations (send, verify, resend) will be added here
 * or in a dedicated OtpV1Controller under /api/otp/v1/otp.
 */
@Slf4j
@RestController
@RequestMapping("/api/otp")
public class OtpController {
    // Future OTP endpoints go here
}

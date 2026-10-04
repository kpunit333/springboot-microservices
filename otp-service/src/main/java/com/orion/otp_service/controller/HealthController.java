package com.orion.otp_service.controller;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

/**
 * Health controller for the otp-service.
 * Mounted at /api/otp — falls under the /api/otp/** gateway route.
 *
 * GET /api/otp/health  → service health check (no auth required)
 */
@Slf4j
@RestController
@RequestMapping("/api/otp")
public class HealthController {

    @GetMapping("/health")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> health() {
        log.debug("Health check requested for otp-service");
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("service", "otp-service");
        status.put("status", "UP");
        status.put("timestamp", OffsetDateTime.now().toString());
        return status;
    }
}

package com.orion.api_gateway.controller;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HealthController {

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> health() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("service", "api-gateway");
        status.put("status", "UP");
        status.put("timestamp", OffsetDateTime.now().toString());
        return status;
    }
}

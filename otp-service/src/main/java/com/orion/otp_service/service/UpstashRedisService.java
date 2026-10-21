package com.orion.otp_service.service;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.orion.otp_service.config.OtpProperties;
import com.orion.otp_service.dto.UpstashResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UpstashRedisService {

    private final RestClient restClient;

    public UpstashRedisService(RestClient.Builder restClientBuilder, OtpProperties properties) {
        this.restClient = restClientBuilder
                .baseUrl(properties.getUpstashRedisRestUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getUpstashRedisRestToken())
                .build();
    }

    /**
     * Stores a key-value pair in Upstash Redis with a time-to-live in seconds.
     */
    public void setWithTtl(String key, String value, long ttlSeconds) {
        try {
            UpstashResponse resp = restClient.post()
                    .uri("/set/{key}/{value}?EX={ttl}", key, value, ttlSeconds)
                    .retrieve()
                    .body(UpstashResponse.class);

            if (resp != null && resp.getError() != null) {
                log.error("Upstash Redis error on SET: {}", resp.getError());
                throw new IllegalStateException("Redis error: " + resp.getError());
            }

            log.debug("Redis SET key '{}' with TTL {}s", key, ttlSeconds);
        } catch (Exception e) {
            log.error("Failed to SET key '{}' in Upstash Redis: {}", key, e.getMessage());
            throw new IllegalStateException("Failed to communicate with Redis: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves the string value associated with a key from Upstash Redis.
     */
    public String get(String key) {
        try {
            UpstashResponse resp = restClient.get()
                    .uri("/get/{key}", key)
                    .retrieve()
                    .body(UpstashResponse.class);

            if (resp == null || resp.getResult() == null) {
                return null;
            }

            return resp.getResultAsString();
        } catch (Exception e) {
            log.error("Failed to GET key '{}' from Upstash Redis: {}", key, e.getMessage());
            throw new IllegalStateException("Failed to communicate with Redis: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes a key from Upstash Redis. Returns true if key was deleted, false otherwise.
     */
    public boolean delete(String key) {
        try {
            UpstashResponse resp = restClient.post()
                    .uri("/del/{key}", key)
                    .retrieve()
                    .body(UpstashResponse.class);

            boolean deleted = resp != null && "1".equals(resp.getResultAsString());
            log.debug("Redis DEL key '{}': {}", key, deleted);
            return deleted;
        } catch (Exception e) {
            log.error("Failed to DEL key '{}' in Upstash Redis: {}", key, e.getMessage());
            throw new IllegalStateException("Failed to communicate with Redis: " + e.getMessage(), e);
        }
    }

    /**
     * Checks if a key exists in Upstash Redis.
     */
    public boolean exists(String key) {
        try {
            UpstashResponse resp = restClient.get()
                    .uri("/exists/{key}", key)
                    .retrieve()
                    .body(UpstashResponse.class);

            return resp != null && "1".equals(resp.getResultAsString());
        } catch (Exception e) {
            log.error("Failed to check EXISTS for key '{}' in Upstash Redis: {}", key, e.getMessage());
            return false;
        }
    }
}

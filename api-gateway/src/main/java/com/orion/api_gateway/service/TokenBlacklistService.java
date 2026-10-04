package com.orion.api_gateway.service;

import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Thread-safe service to blacklist invalidated tokens upon logout.
 * Designed with clean interfaces so it can be easily backed by Redis in distributed environments.
 */
@Service
@Slf4j
public class TokenBlacklistService {

    private final Map<String, Instant> blacklist = new ConcurrentHashMap<>();

    /**
     * Add token to blacklist until its natural expiration time.
     */
    public void blacklistToken(String token, Instant expiresAt) {
        if (token != null && expiresAt != null && expiresAt.isAfter(Instant.now())) {
            blacklist.put(token, expiresAt);
            log.debug("Token blacklisted until {}", expiresAt);
        }
    }

    /**
     * Check whether token is blacklisted.
     */
    public boolean isBlacklisted(String token) {
        if (token == null) {
            return false;
        }
        Instant expiresAt = blacklist.get(token);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt.isBefore(Instant.now())) {
            blacklist.remove(token);
            return false;
        }
        return true;
    }

    /**
     * Evict expired tokens every 10 minutes to prevent memory leaks.
     */
    @Scheduled(fixedRate = 600000)
    public void cleanupExpiredTokens() {
        Instant now = Instant.now();
        int initialSize = blacklist.size();
        Iterator<Map.Entry<String, Instant>> it = blacklist.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Instant> entry = it.next();
            if (entry.getValue().isBefore(now)) {
                it.remove();
            }
        }
        int removed = initialSize - blacklist.size();
        if (removed > 0) {
            log.info("Cleaned up {} expired tokens from blacklist", removed);
        }
    }
}

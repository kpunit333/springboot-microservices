package com.orion.otp_service.filter;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orion.otp_service.config.OtpIpProperties;
import com.orion.otp_service.dto.ApiResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Filter that restricts access to sensitive OTP endpoints:
 * - POST /api/otp/v1/generate
 * - POST /api/otp/v1/verify
 *
 * Test endpoints (/api/otp/v1/test/generate, /api/otp/v1/test/verify)
 * and monitoring endpoints (/actuator/**, /health) are unrestricted.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class OtpIpFilter extends OncePerRequestFilter {

    private final OtpIpProperties properties;
    private final ObjectMapper objectMapper;

    public OtpIpFilter(OtpIpProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = extractNormalizedPath(request);

        // If not a restricted endpoint, allow without IP restriction
        if (!isRestrictedEndpoint(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // If IP filtering is disabled via configuration, bypass restriction
        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Allow CORS preflight OPTIONS requests
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = extractClientIp(request);

        if (isIpAllowed(clientIp)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("Access Denied to restricted OTP endpoint '{}': ClientIP='{}'", path, clientIp);
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ApiResponse<Void> errorResponse = ApiResponse.error(
                    "Access denied: IP [" + (clientIp != null ? clientIp : "UNKNOWN")
                            + "] is not allowed to access this endpoint");
            objectMapper.writeValue(response.getWriter(), errorResponse);
        }
    }

    /**
     * Determines whether the given request path is subject to IP whitelisting.
     * Restricted:
     * - /api/otp/v1/generate
     * - /api/otp/v1/verify
     *
     * Unrestricted:
     * - /api/otp/v1/test/generate
     * - /api/otp/v1/test/verify
     * - /actuator/**
     * - /health/**
     */
    boolean isRestrictedEndpoint(String path) {
        if (path == null) {
            return false;
        }

        // Explicitly exempt all test endpoints
        if (path.equals("/api/otp/v1/test/generate")
                || path.equals("/api/otp/v1/test/verify")
                || path.startsWith("/api/otp/v1/test/")
                || path.startsWith("/actuator") || path.startsWith("/health")) {
            return false;
        }

        // Only restrict production generate and verify endpoints
        return path.equals("/api/otp/v1/generate")
                || path.equals("/api/otp/v1/verify");
    }

    /**
     * Normalizes request URI by stripping servlet context path and trailing
     * slashes.
     */
    private String extractNormalizedPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null) {
            return "";
        }
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    /**
     * Extracts client IP address considering proxy headers.
     */
    String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.trim().isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.trim().isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Checks if the client IP is present in the allowed IPs list.
     */
    boolean isIpAllowed(String clientIp) {
        List<String> whitelist = properties.getIps();
        if (whitelist == null || whitelist.isEmpty()) {
            return false;
        }

        if (clientIp == null || clientIp.trim().isEmpty()) {
            return false;
        }

        String normalizedClientIp = clientIp.trim();
        // Strip IPv4 port if present (e.g. 127.0.0.1:54321)
        if (normalizedClientIp.contains(":") && !normalizedClientIp.contains("::")) {
            normalizedClientIp = normalizedClientIp.substring(0, normalizedClientIp.indexOf(':'));
        }

        for (String entry : whitelist) {
            if (entry == null || entry.trim().isEmpty()) {
                continue;
            }
            String cleanEntry = entry.trim();

            // Strip port if configured with port (e.g. 127.0.0.1:3000)
            String ipPart = cleanEntry.contains(":") && !cleanEntry.contains("::")
                    ? cleanEntry.substring(0, cleanEntry.indexOf(':'))
                    : cleanEntry;

            // Direct match
            if (normalizedClientIp.equalsIgnoreCase(cleanEntry) || normalizedClientIp.equalsIgnoreCase(ipPart)) {
                return true;
            }

            // Loopback equivalence (localhost, 127.0.0.1, ::1, 0:0:0:0:0:0:0:1)
            if (isLoopback(normalizedClientIp) && isLoopback(ipPart)) {
                return true;
            }

            // CIDR matching (e.g. 192.168.1.0/24 or 10.0.0.0/8)
            if (cleanEntry.contains("/") && isCidrMatch(normalizedClientIp, cleanEntry)) {
                return true;
            }

            // InetAddress host resolution match
            if (isHostMatch(normalizedClientIp, ipPart)) {
                return true;
            }
        }

        return false;
    }

    private boolean isLoopback(String ip) {
        if ("127.0.0.1".equals(ip) || "localhost".equalsIgnoreCase(ip)
                || "::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            return true;
        }
        try {
            InetAddress address = InetAddress.getByName(ip);
            return address.isLoopbackAddress();
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isHostMatch(String clientIp, String hostOrIp) {
        try {
            InetAddress resolved = InetAddress.getByName(hostOrIp);
            if (resolved.getHostAddress().equalsIgnoreCase(clientIp)) {
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private boolean isCidrMatch(String clientIp, String cidr) {
        try {
            String[] parts = cidr.split("/");
            if (parts.length != 2) {
                return false;
            }
            InetAddress target = InetAddress.getByName(parts[0]);
            InetAddress client = InetAddress.getByName(clientIp);

            byte[] targetBytes = target.getAddress();
            byte[] clientBytes = client.getAddress();

            if (targetBytes.length != clientBytes.length) {
                return false;
            }

            int prefixLength = Integer.parseInt(parts[1]);
            int fullBytes = prefixLength / 8;
            int remainderBits = prefixLength % 8;

            for (int i = 0; i < fullBytes; i++) {
                if (targetBytes[i] != clientBytes[i]) {
                    return false;
                }
            }

            if (remainderBits > 0 && fullBytes < targetBytes.length) {
                int mask = (0xFF << (8 - remainderBits)) & 0xFF;
                if ((targetBytes[fullBytes] & mask) != (clientBytes[fullBytes] & mask)) {
                    return false;
                }
            }

            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}

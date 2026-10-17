package com.orion.api_gateway.filter;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.orion.api_gateway.config.IpWhitelistProperties;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IpWhitelistFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(IpWhitelistFilter.class);

    private final IpWhitelistProperties properties;

    public IpWhitelistFilter(IpWhitelistProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Always allow CORS preflight requests so browser preflight OPTIONS is not blocked
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = extractClientIp(request);
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        String referer = request.getHeader(HttpHeaders.REFERER);

        boolean ipAllowed = isIpAllowed(clientIp);
        boolean originAllowed = isOriginAllowed(origin, referer);

        if (ipAllowed && originAllowed) {
            filterChain.doFilter(request, response);
        } else {
            String rejectedSource = origin != null ? origin : clientIp;
            log.warn("Access Denied by IP/Origin Whitelist: ClientIP='{}', Origin='{}', Referer='{}', Path='{}'",
                    clientIp, origin, referer, request.getRequestURI());

            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(String.format(
                    "{\"timestamp\":\"%s\",\"status\":403,\"error\":\"Forbidden\",\"message\":\"Access denied: Source [%s] is not in the allowed IP/Origin whitelist\",\"path\":\"%s\"}",
                    Instant.now(),
                    rejectedSource,
                    request.getRequestURI()
            ));
        }
    }

    /**
     * Checks if the client's remote IP is allowed.
     */
    private boolean isIpAllowed(String clientIp) {
        List<String> whitelist = properties.getIps();
        if (whitelist == null || whitelist.isEmpty()) {
            return true;
        }

        if (clientIp == null || clientIp.trim().isEmpty()) {
            return false;
        }

        for (String entry : whitelist) {
            if (entry == null || entry.trim().isEmpty()) {
                continue;
            }
            String cleanEntry = entry.trim();
            // If entry contains a port like "127.0.0.1:3000", extract the IP portion
            String ipPart = cleanEntry.contains(":") && !cleanEntry.contains("::")
                    ? cleanEntry.substring(0, cleanEntry.indexOf(':'))
                    : cleanEntry;

            if (clientIp.equalsIgnoreCase(cleanEntry) || clientIp.equalsIgnoreCase(ipPart)) {
                return true;
            }

            // Support IPv6 localhost / IPv4 localhost matching
            if (("127.0.0.1".equals(ipPart) || "localhost".equalsIgnoreCase(ipPart)) &&
                    ("0:0:0:0:0:0:0:1".equals(clientIp) || "::1".equals(clientIp))) {
                return true;
            }
        }

        return false;
    }

    /**
     * If an Origin or Referer header is present (browser requests), verifies it matches the whitelist.
     */
    private boolean isOriginAllowed(String origin, String referer) {
        List<String> whitelist = properties.getIps();
        if (whitelist == null || whitelist.isEmpty()) {
            return true;
        }

        // If neither Origin nor Referer is present (e.g. direct server-to-server or curl), origin check passes
        if ((origin == null || origin.trim().isEmpty()) && (referer == null || referer.trim().isEmpty())) {
            return true;
        }

        for (String entry : whitelist) {
            if (entry == null || entry.trim().isEmpty()) {
                continue;
            }
            String cleanEntry = entry.trim();

            if (origin != null && matchesOrigin(origin, cleanEntry)) {
                return true;
            }

            if (referer != null && matchesOrigin(referer, cleanEntry)) {
                return true;
            }
        }

        return false;
    }

    private boolean matchesOrigin(String originOrReferer, String entry) {
        try {
            URI uri = URI.create(originOrReferer);
            String host = uri.getHost();
            int port = uri.getPort();
            String hostWithPort = port > 0 ? (host + ":" + port) : host;

            if (entry.equalsIgnoreCase(originOrReferer) ||
                    (hostWithPort != null && entry.equalsIgnoreCase(hostWithPort)) ||
                    (host != null && entry.equalsIgnoreCase(host))) {
                return true;
            }

            if (originOrReferer.equalsIgnoreCase("http://" + entry) ||
                    originOrReferer.equalsIgnoreCase("https://" + entry)) {
                return true;
            }
        } catch (Exception ignored) {
            if (originOrReferer.contains(entry)) {
                return true;
            }
        }
        return false;
    }

    private String extractClientIp(HttpServletRequest request) {
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
}

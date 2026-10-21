package com.orion.otp_service.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.orion.otp_service.config.OtpProperties;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class OrganizationServiceClient {

    private final RestClient restClient;
    private final OtpProperties properties;

    public OrganizationServiceClient(
            @Qualifier("loadBalanced") RestClient.Builder loadBalancedBuilder,
            OtpProperties properties) {
        this.restClient = loadBalancedBuilder
                .baseUrl("http://organization-service")
                .build();
        this.properties = properties;
    }

    /**
     * Single validation call: verifies secretKey is valid, purposeId is valid,
     * and both belong to the same organization.
     */
    public com.orion.otp_service.dto.ValidationResponse validateProjectAndPurpose(String secretKey, String purposeId) {
        if (!properties.isValidationEnabled()) {
            return com.orion.otp_service.dto.ValidationResponse.builder()
                    .valid(true)
                    .message("Validation disabled")
                    .build();
        }
        if (secretKey == null || secretKey.trim().isEmpty()) {
            return com.orion.otp_service.dto.ValidationResponse.builder()
                    .valid(false)
                    .message("Secret key is required")
                    .build();
        }
        if (purposeId == null || purposeId.trim().isEmpty()) {
            return com.orion.otp_service.dto.ValidationResponse.builder()
                    .valid(false)
                    .message("Purpose ID is required")
                    .build();
        }

        try {
            com.orion.otp_service.dto.ValidationResponse resp = restClient.get()
                    .uri("/api/organizations/internal/v1/validate?secretKey={secretKey}&purposeId={purposeId}",
                            secretKey.trim(), purposeId.trim())
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        log.warn("Organization service returned {} when validating secretKey='{}' and purposeId='{}'",
                                res.getStatusCode(), secretKey, purposeId);
                    })
                    .body(com.orion.otp_service.dto.ValidationResponse.class);

            return resp != null ? resp : com.orion.otp_service.dto.ValidationResponse.builder()
                    .valid(false)
                    .message("Empty response from organization service")
                    .build();
        } catch (Exception e) {
            log.warn("Could not reach organization-service to validate secretKey='{}' and purposeId='{}': {}. Strict mode: {}",
                    secretKey, purposeId, e.getMessage(), properties.isValidationStrict());
            if (properties.isValidationStrict()) {
                throw new IllegalStateException("Organization service is unavailable to validate project and purpose", e);
            }
            // In non-strict mode, allow valid non-empty identifier format
            boolean formatValid = secretKey.trim().length() >= 2 && purposeId.trim().length() >= 2;
            return com.orion.otp_service.dto.ValidationResponse.builder()
                    .valid(formatValid)
                    .message(formatValid ? "Fallback format validation passed" : "Invalid identifier format")
                    .build();
        }
    }
}

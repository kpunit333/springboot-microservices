package com.orion.api_gateway.client;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.orion.api_gateway.client.dto.OrgInternalDto;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class OrganizationServiceClient {

    private final RestClient restClient;

    public OrganizationServiceClient(@Qualifier("loadBalanced") RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl("http://organization-service")
                .build();
    }

    public Optional<OrgInternalDto> getOrganizationByCode(String code) {
        return getOrganizationByIdentifier(code);
    }

    public Optional<OrgInternalDto> getOrganizationByName(String name) {
        try {
            OrgInternalDto org = restClient.get()
                    .uri("/api/organizations/internal/v1/organization/by-name/{name}", name)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        log.warn("Organization service returned {} for name: {}", response.getStatusCode(), name);
                    })
                    .body(OrgInternalDto.class);

            return Optional.ofNullable(org);
        } catch (Exception e) {
            log.error("Error communicating with organization-service for name '{}': {}", name, e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<OrgInternalDto> getOrganizationByIdentifier(String identifier) {
        try {
            OrgInternalDto org = restClient.get()
                    .uri("/api/organizations/internal/v1/organization/{identifier}", identifier)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        log.warn("Organization service returned {} for identifier: {}", response.getStatusCode(), identifier);
                    })
                    .body(OrgInternalDto.class);

            return Optional.ofNullable(org);
        } catch (Exception e) {
            log.error("Error communicating with organization-service for identifier '{}': {}", identifier, e.getMessage());
            return Optional.empty();
        }
    }
}

package com.orion.organization_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.orion.organization_service.dto.OrganizationInternalDto;
import com.orion.organization_service.service.OrganizationService;

import lombok.extern.slf4j.Slf4j;

/**
 * Main controller for the organization-service, mounted at /api/organizations.
 *
 * Responsibilities:
 * - Internal endpoints consumed by the API Gateway (org login auth).
 *
 * Auth notes:
 * Internal endpoints are NOT meant for external clients — protect at the
 * network / gateway level.
 */
@Slf4j
@RestController
@RequestMapping("/api/organizations/internal/v1")
public class OrganizationController {

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private com.orion.organization_service.service.ProjectService projectService;

    @Autowired
    private com.orion.organization_service.service.PurposeService purposeService;

    /**
     * GET /api/organizations/internal/v1/organization/{identifier}
     * Used by the API Gateway to load org credentials by code or email during
     * login/token refresh.
     */
    @GetMapping("/organization/{identifier}")
    @ResponseStatus(HttpStatus.OK)
    public OrganizationInternalDto getByIdentifierForAuth(@PathVariable String identifier) {
        log.debug("Internal auth lookup for organization identifier: {}", identifier);
        return organizationService.getByIdentifierForAuth(identifier);
    }

    /**
     * GET /api/organizations/internal/v1/organization/by-email/{email}
     * Used by the API Gateway to load org credentials explicitly by organization
     * email.
     */
    @GetMapping("/organization/by-email/{email}")
    @ResponseStatus(HttpStatus.OK)
    public OrganizationInternalDto getByEmailForAuth(@PathVariable String email) {
        log.debug("Internal auth lookup for organization email: {}", email);
        return organizationService.getByEmailForAuth(email);
    }

    /**
     * GET
     * /api/organizations/internal/v1/validate?secretKey={secretKey}&purposeId={purposeId}
     *
     * Single internal API used by otp-service to validate that:
     * 1. project secretKey exists and is active/not expired
     * 2. purposeId exists and is not expired
     * 3. both project and purpose belong to the same organization
     */
    @GetMapping("/validate")
    @ResponseStatus(HttpStatus.OK)
    public com.orion.organization_service.dto.ValidationResponse validateProjectAndPurpose(
            @RequestParam("secretKey") String secretKey,
            @RequestParam("purposeId") String purposeId) {
        log.debug("Internal validation for secretKey='{}' and purposeId='{}'", secretKey, purposeId);
        return organizationService.validateProjectAndPurpose(secretKey, purposeId);
    }
}

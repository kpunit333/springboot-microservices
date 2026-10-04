package com.orion.organization_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orion.organization_service.dto.ApiResponse;
import com.orion.organization_service.dto.OrganizationRequest;
import com.orion.organization_service.dto.OrganizationResponse;
import com.orion.organization_service.service.OrganizationService;

import lombok.extern.slf4j.Slf4j;

/**
 * V1 CRUD controller for organization operations.
 * Mounted at /api/organizations/v1/organization — falls under the
 * /api/organizations/** gateway route.
 */
@Slf4j
@RestController
@RequestMapping("/api/organizations/v1/organization")
public class OrganizationV1Controller {

    @Autowired
    private OrganizationService organizationService;

    /**
     * POST /api/organizations/v1/organization
     * Create a new organization. No authentication required.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OrganizationResponse>> create(@RequestBody OrganizationRequest request) {
        log.info("Creating organization: name={}", request.getName());
        OrganizationResponse response = organizationService.create(request);
        log.info("Organization created: code={}", response.getCode());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Organization created successfully", response));
    }

    /**
     * PATCH/PUT /api/organizations/v1/organization
     * Update an existing organization using code and password. Auth required.
     */
    @PatchMapping
    public ResponseEntity<ApiResponse<OrganizationResponse>> updatePatch(@RequestBody OrganizationRequest request) {
        return update(request);
    }

    @org.springframework.web.bind.annotation.PutMapping
    public ResponseEntity<ApiResponse<OrganizationResponse>> update(@RequestBody OrganizationRequest request) {
        log.info("Updating organization: code={}", request.getCode());
        OrganizationResponse response = organizationService.update(request);
        log.info("Organization updated: code={}", response.getCode());
        return ResponseEntity.ok(ApiResponse.ok("Organization updated successfully", response));
    }

    /**
     * GET /api/organizations/v1/organization/{code}
     * Fetch an organization by its unique code. Auth required.
     */
    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<OrganizationResponse>> getByCode(@PathVariable String code) {
        log.info("Fetching organization: code={}", code);
        OrganizationResponse response = organizationService.getByCode(code);
        return ResponseEntity.ok(ApiResponse.ok("Organization fetched successfully", response));
    }

    /**
     * DELETE /api/organizations/v1/organization/{code}
     * Delete an organization by its unique code. Auth required.
     */
    @DeleteMapping("/{code}")
    public ResponseEntity<ApiResponse<Void>> deleteByCode(@PathVariable String code) {
        log.info("Deleting organization: code={}", code);
        organizationService.deleteByCode(code);
        log.info("Organization deleted: code={}", code);
        return ResponseEntity.ok(ApiResponse.ok("Organization deleted successfully", null));
    }
}

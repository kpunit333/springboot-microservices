package com.orion.organization_service.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orion.organization_service.dto.ApiResponse;
import com.orion.organization_service.dto.PurposeRequest;
import com.orion.organization_service.dto.PurposeResponse;
import com.orion.organization_service.service.PurposeService;

import lombok.extern.slf4j.Slf4j;

/**
 * V1 CRUD controller for organization-scoped purpose operations.
 *
 * Endpoints:
 * GET /api/organizations/v1/{organizationCode}/purpose - list all purposes for
 * that organization
 * GET /api/organizations/v1/{organizationCode}/purpose/{purposeCode} - get
 * details of that particular purpose
 * POST /api/organizations/v1/{organizationCode}/purpose/ - create a purpose
 * PATCH /api/organizations/v1/{organizationCode}/purpose/{purposeCode} - update
 * a particular purpose
 * DELETE /api/organizations/v1/{organizationCode}/purpose/{purposeCode} -
 * delete that particular purpose
 */
@Slf4j
@RestController
@RequestMapping("/api/organizations/v1/{organizationCode}/purpose")
public class PurposeV1Controller {

    @Autowired
    private PurposeService purposeService;

    /**
     * GET /api/organizations/v1/{organizationCode}/purpose
     * Get a list of all purposes for that organization.
     */
    @GetMapping({ "", "/" })
    public ResponseEntity<ApiResponse<List<PurposeResponse>>> getAll(
            @PathVariable String organizationCode) {
        log.info("Fetching all purposes for organization: code={}", organizationCode);
        List<PurposeResponse> list = purposeService.getPurposesByOrganization(organizationCode);
        return ResponseEntity.ok(ApiResponse.ok("Purposes fetched successfully", list));
    }

    /**
     * GET /api/organizations/v1/{organizationCode}/purpose/{purposeCode}
     * Get details of that particular purpose.
     */
    @GetMapping("/{purposeCode}")
    public ResponseEntity<ApiResponse<PurposeResponse>> getByCode(
            @PathVariable String organizationCode,
            @PathVariable String purposeCode) {
        log.info("Fetching purpose: purposeCode={}, organizationCode={}", purposeCode, organizationCode);
        PurposeResponse response = purposeService.getByCodeAndOrganization(organizationCode, purposeCode);
        return ResponseEntity.ok(ApiResponse.ok("Purpose fetched successfully", response));
    }

    /**
     * POST /api/organizations/v1/{organizationCode}/purpose/
     * Create a purpose for that organization.
     */
    @PostMapping({ "", "/" })
    public ResponseEntity<ApiResponse<PurposeResponse>> create(
            @PathVariable String organizationCode,
            @RequestBody PurposeRequest request) {
        log.info("Creating purpose for organization: code={}, name={}",
                organizationCode, request != null ? request.getName() : null);
        PurposeResponse response = purposeService.createForOrganization(organizationCode, request);
        log.info("Purpose created: code={}, organizationCode={}", response.getCode(), organizationCode);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Purpose created successfully", response));
    }

    /**
     * PATCH /api/organizations/v1/{organizationCode}/purpose/{purposeCode}
     * Update a particular purpose.
     */
    @PatchMapping("/{purposeCode}")
    public ResponseEntity<ApiResponse<PurposeResponse>> updatePatch(
            @PathVariable String organizationCode,
            @PathVariable String purposeCode,
            @RequestBody PurposeRequest request) {
        return update(organizationCode, purposeCode, request);
    }

    /**
     * PUT /api/organizations/v1/{organizationCode}/purpose/{purposeCode}
     * Update a particular purpose.
     */
    @PutMapping("/{purposeCode}")
    public ResponseEntity<ApiResponse<PurposeResponse>> update(
            @PathVariable String organizationCode,
            @PathVariable String purposeCode,
            @RequestBody PurposeRequest request) {
        log.info("Updating purpose: purposeCode={}, organizationCode={}", purposeCode, organizationCode);
        PurposeResponse response = purposeService.updateForOrganization(organizationCode, purposeCode, request);
        log.info("Purpose updated: code={}, organizationCode={}", response.getCode(), organizationCode);
        return ResponseEntity.ok(ApiResponse.ok("Purpose updated successfully", response));
    }

    /**
     * DELETE /api/organizations/v1/{organizationCode}/purpose/{purposeCode}
     * Delete that particular purpose.
     */
    @DeleteMapping("/{purposeCode}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable String organizationCode,
            @PathVariable String purposeCode) {
        log.info("Deleting purpose: purposeCode={}, organizationCode={}", purposeCode, organizationCode);
        purposeService.deleteForOrganization(organizationCode, purposeCode);
        log.info("Purpose deleted: purposeCode={}, organizationCode={}", purposeCode, organizationCode);
        return ResponseEntity.ok(ApiResponse.ok("Purpose deleted successfully", null));
    }
}

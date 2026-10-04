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
import com.orion.organization_service.dto.ProjectRequest;
import com.orion.organization_service.dto.ProjectResponse;
import com.orion.organization_service.service.ProjectService;

import lombok.extern.slf4j.Slf4j;

/**
 * V1 CRUD controller for organization-scoped project operations.
 *
 * Endpoints:
 *   POST   /v1/{organizationCode}/project               - create new project of that particular organization
 *   GET    /v1/{organizationCode}/project               - gives list of all project of that particular organization
 *   GET    /v1/{organizationCode}/project/{projectCode} - gives data of particular project of that particular organization
 *   PATCH  /v1/{organizationCode}/project/{projectCode} - update that particular project of that particular organization
 *   DELETE /v1/{organizationCode}/project/{projectCode} - delete project of that particular organization
 *
 * Also supports /api/organizations/v1/{organizationCode}/project for routing via API Gateway.
 */
@Slf4j
@RestController
@RequestMapping("/api/organizations/v1/{organizationCode}/project")
public class ProjectV1Controller {

    @Autowired
    private ProjectService projectService;

    /**
     * POST /v1/{organizationCode}/project
     * Create a new project of that particular organization.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ProjectResponse>> create(
            @PathVariable String organizationCode,
            @RequestBody ProjectRequest request) {
        log.info("Creating project for organization: code={}, name={}", organizationCode, request != null ? request.getName() : null);
        ProjectResponse response = projectService.createForOrganization(organizationCode, request);
        log.info("Project created: code={}, organizationCode={}", response.getCode(), organizationCode);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Project created successfully", response));
    }

    /**
     * GET /v1/{organizationCode}/project
     * Gives list of all projects of that particular organization.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getAll(
            @PathVariable String organizationCode) {
        log.info("Fetching all projects for organization: code={}", organizationCode);
        List<ProjectResponse> list = projectService.getProjectsByOrganization(organizationCode);
        return ResponseEntity.ok(ApiResponse.ok("Projects fetched successfully", list));
    }

    /**
     * GET /v1/{organizationCode}/project/{projectCode}
     * Gives data of particular project of that particular organization.
     */
    @GetMapping("/{projectCode}")
    public ResponseEntity<ApiResponse<ProjectResponse>> getByCode(
            @PathVariable String organizationCode,
            @PathVariable String projectCode) {
        log.info("Fetching project: projectCode={}, organizationCode={}", projectCode, organizationCode);
        ProjectResponse response = projectService.getByCodeAndOrganization(organizationCode, projectCode);
        return ResponseEntity.ok(ApiResponse.ok("Project fetched successfully", response));
    }

    /**
     * PATCH /v1/{organizationCode}/project/{projectCode}
     * Update that particular project of that particular organization.
     */
    @PatchMapping("/{projectCode}")
    public ResponseEntity<ApiResponse<ProjectResponse>> updatePatch(
            @PathVariable String organizationCode,
            @PathVariable String projectCode,
            @RequestBody ProjectRequest request) {
        return update(organizationCode, projectCode, request);
    }

    @PutMapping("/{projectCode}")
    public ResponseEntity<ApiResponse<ProjectResponse>> update(
            @PathVariable String organizationCode,
            @PathVariable String projectCode,
            @RequestBody ProjectRequest request) {
        log.info("Updating project: projectCode={}, organizationCode={}", projectCode, organizationCode);
        ProjectResponse response = projectService.updateForOrganization(organizationCode, projectCode, request);
        log.info("Project updated: code={}, organizationCode={}", response.getCode(), organizationCode);
        return ResponseEntity.ok(ApiResponse.ok("Project updated successfully", response));
    }

    /**
     * DELETE /v1/{organizationCode}/project/{projectCode}
     * Delete project of that particular organization.
     */
    @DeleteMapping("/{projectCode}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable String organizationCode,
            @PathVariable String projectCode) {
        log.info("Deleting project: projectCode={}, organizationCode={}", projectCode, organizationCode);
        projectService.deleteForOrganization(organizationCode, projectCode);
        log.info("Project deleted: projectCode={}, organizationCode={}", projectCode, organizationCode);
        return ResponseEntity.ok(ApiResponse.ok("Project deleted successfully", null));
    }
}

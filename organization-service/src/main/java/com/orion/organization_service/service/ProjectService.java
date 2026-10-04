package com.orion.organization_service.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orion.organization_service.dto.ProjectRequest;
import com.orion.organization_service.dto.ProjectResponse;
import com.orion.organization_service.model.Organization;
import com.orion.organization_service.model.Project;
import com.orion.organization_service.repository.OrganizationRepository;
import com.orion.organization_service.repository.ProjectRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    /**
     * Fetch all projects across all organizations.
     */
    public List<ProjectResponse> getAll() {
        log.debug("Fetching all projects");
        List<Project> all = projectRepository.findAll();
        log.debug("Found {} projects", all.size());
        return all.stream().map(this::mapToResponse).toList();
    }

    /**
     * GET /v1/{organizationCode}/project
     * List all projects belonging to the specified organization.
     */
    public List<ProjectResponse> getProjectsByOrganization(String organizationCode) {
        log.debug("Fetching projects for organization: code={}", organizationCode);
        Organization organization = getOrganizationOrThrow(organizationCode);
        List<Project> projects = projectRepository.findByOrganizationId(organization.getId());
        log.debug("Found {} projects for organization: code={}", projects.size(), organizationCode);
        return projects.stream().map(this::mapToResponse).toList();
    }

    /**
     * POST /v1/{organizationCode}/project
     * Create a new project for a particular organization.
     */
    @Transactional
    public ProjectResponse createForOrganization(String organizationCode, ProjectRequest request) {
        log.debug("Creating project for organization: code={}, projectName={}", organizationCode, request != null ? request.getName() : null);
        if (request == null) {
            throw new IllegalArgumentException("Project request body is required");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Project name is required");
        }

        Organization organization = getOrganizationOrThrow(organizationCode);

        Project project = Project.builder()
                .organizationId(organization.getId())
                .name(request.getName().trim())
                .description(request.getDescription())
                .build();

        project = projectRepository.save(project);
        log.info("Project saved: code={}, organizationCode={}, secretKey={}", project.getCode(), organizationCode, project.getSecretKey());
        return this.mapToResponse(project);
    }

    /**
     * Alias for createForOrganization(organizationCode, request).
     */
    @Transactional
    public ProjectResponse create(String organizationCode, ProjectRequest request) {
        return createForOrganization(organizationCode, request);
    }

    /**
     * GET /v1/{organizationCode}/project/{projectCode}
     * Fetch a specific project by code under the given organization.
     */
    public ProjectResponse getByCodeAndOrganization(String organizationCode, String projectCode) {
        log.debug("Fetching project: projectCode={}, organizationCode={}", projectCode, organizationCode);
        Organization organization = getOrganizationOrThrow(organizationCode);
        Project project = projectRepository.findByCodeAndOrganizationId(projectCode, organization.getId())
                .orElseThrow(() -> {
                    log.warn("Project not found: code={} in organization={}", projectCode, organizationCode);
                    return new IllegalArgumentException("Project not found with code: " + projectCode + " in organization: " + organizationCode);
                });
        return this.mapToResponse(project);
    }

    /**
     * PATCH /v1/{organizationCode}/project/{projectCode}
     * Update a particular project of that particular organization.
     */
    @Transactional
    public ProjectResponse updateForOrganization(String organizationCode, String projectCode, ProjectRequest request) {
        log.debug("Updating project: projectCode={}, organizationCode={}", projectCode, organizationCode);
        if (projectCode == null || projectCode.isBlank()) {
            throw new IllegalArgumentException("Project code is required for update");
        }
        Organization organization = getOrganizationOrThrow(organizationCode);
        Project project = projectRepository.findByCodeAndOrganizationId(projectCode, organization.getId())
                .orElseThrow(() -> {
                    log.warn("Project not found for update: code={} in organization={}", projectCode, organizationCode);
                    return new IllegalArgumentException("Project not found with code: " + projectCode + " in organization: " + organizationCode);
                });

        if (request != null) {
            if (request.getName() != null && !request.getName().isBlank()) {
                project.setName(request.getName().trim());
            }
            if (request.getDescription() != null) {
                project.setDescription(request.getDescription());
            }
        }

        project = projectRepository.save(project);
        log.info("Project updated: code={}, organizationCode={}", project.getCode(), organizationCode);
        return this.mapToResponse(project);
    }

    /**
     * Overloaded update(ProjectRequest request) for backward compatibility.
     */
    @Transactional
    public ProjectResponse update(ProjectRequest request) {
        if (request == null || request.getCode() == null || request.getCode().isBlank()) {
            throw new IllegalArgumentException("Project code is required for update");
        }
        Project project = projectRepository.findByCode(request.getCode())
                .orElseThrow(() -> {
                    log.warn("Update failed — project not found with code: {}", request.getCode());
                    return new IllegalArgumentException("Project not found with code: " + request.getCode());
                });

        if (request.getName() != null && !request.getName().isBlank()) {
            project.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }

        project = projectRepository.save(project);
        log.info("Project updated: code={}", project.getCode());
        return this.mapToResponse(project);
    }

    /**
     * DELETE /v1/{organizationCode}/project/{projectCode}
     * Delete a project of that particular organization.
     */
    @Transactional
    public void deleteForOrganization(String organizationCode, String projectCode) {
        log.debug("Deleting project: projectCode={}, organizationCode={}", projectCode, organizationCode);
        Organization organization = getOrganizationOrThrow(organizationCode);
        Project project = projectRepository.findByCodeAndOrganizationId(projectCode, organization.getId())
                .orElseThrow(() -> {
                    log.warn("Project not found for deletion: code={} in organization={}", projectCode, organizationCode);
                    return new IllegalArgumentException("Project not found with code: " + projectCode + " in organization: " + organizationCode);
                });
        projectRepository.delete(project);
        log.info("Project deleted: code={}, organizationCode={}", projectCode, organizationCode);
    }

    public ProjectResponse getByCode(String code) {
        log.debug("Looking up project: code={}", code);
        Project project = projectRepository.findByCode(code)
                .orElseThrow(() -> {
                    log.warn("Project not found with code: {}", code);
                    return new IllegalArgumentException("Project not found with code: " + code);
                });
        return this.mapToResponse(project);
    }

    public ProjectResponse getByIdentifier(String identifier) {
        log.debug("Looking up project: {}", identifier);
        Project project = findProjectByIdentifier(identifier)
                .orElseThrow(() -> {
                    log.warn("Project not found: {}", identifier);
                    return new IllegalArgumentException("Project not found with identifier: " + identifier);
                });
        return this.mapToResponse(project);
    }

    @Transactional
    public void deleteByCode(String code) {
        log.debug("Deleting project: code={}", code);
        Project project = projectRepository.findByCode(code)
                .orElseThrow(() -> {
                    log.warn("Delete failed — project not found with code: {}", code);
                    return new IllegalArgumentException("Project not found with code: " + code);
                });
        projectRepository.delete(project);
        log.info("Project deleted: code={}", project.getCode());
    }

    @Transactional
    public void deleteByIdentifier(String identifier) {
        log.debug("Deleting project: {}", identifier);
        Project project = findProjectByIdentifier(identifier)
                .orElseThrow(() -> {
                    log.warn("Delete failed — project not found: {}", identifier);
                    return new IllegalArgumentException("Project not found with identifier: " + identifier);
                });
        projectRepository.delete(project);
        log.info("Project deleted: code={}", project.getCode());
    }

    private Organization getOrganizationOrThrow(String organizationCode) {
        if (organizationCode == null || organizationCode.isBlank()) {
            throw new IllegalArgumentException("Organization code is required");
        }
        return organizationRepository.findByCode(organizationCode)
                .orElseThrow(() -> {
                    log.warn("Organization not found with code: {}", organizationCode);
                    return new IllegalArgumentException("Organization not found with code: " + organizationCode);
                });
    }

    private Optional<Project> findProjectByIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return Optional.empty();
        }
        Optional<Project> byCode = projectRepository.findByCode(identifier);
        if (byCode.isPresent()) {
            return byCode;
        }
        try {
            Long id = Long.parseLong(identifier);
            return projectRepository.findById(id);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private ProjectResponse mapToResponse(Project project) {
        return ProjectResponse.builder()
                .code(project.getCode())
                .name(project.getName())
                .secretKey(project.getSecretKey())
                .status(project.getStatus())
                .createdAt(project.getCreatedAt())
                .description(project.getDescription())
                .build();
    }
}

package com.orion.organization_service.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orion.organization_service.dto.PurposeRequest;
import com.orion.organization_service.dto.PurposeResponse;
import com.orion.organization_service.model.Organization;
import com.orion.organization_service.model.Purpose;
import com.orion.organization_service.repository.OrganizationRepository;
import com.orion.organization_service.repository.PurposeRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PurposeService {

    @Autowired
    private PurposeRepository purposeRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    /**
     * GET /api/organizations/v1/{organizationCode}/purpose
     * List all purposes belonging to the specified organization.
     */
    public List<PurposeResponse> getPurposesByOrganization(String organizationCode) {
        log.debug("Fetching purposes for organization: code={}", organizationCode);
        Organization organization = getOrganizationOrThrow(organizationCode);
        List<Purpose> list = purposeRepository.findByOrganizationId(organization.getId());
        log.debug("Found {} purposes for organization: code={}", list.size(), organizationCode);
        return list.stream().map(this::mapToResponse).toList();
    }

    /**
     * GET /api/organizations/v1/{organizationCode}/purpose/{purposeCode}
     * Get details of a particular purpose.
     */
    public PurposeResponse getByCodeAndOrganization(String organizationCode, String purposeCode) {
        log.debug("Fetching purpose: purposeCode={}, organizationCode={}", purposeCode, organizationCode);
        if (purposeCode == null || purposeCode.isBlank()) {
            throw new IllegalArgumentException("Purpose code is required");
        }
        Organization organization = getOrganizationOrThrow(organizationCode);
        Purpose purpose = purposeRepository.findByCodeAndOrganizationId(purposeCode, organization.getId())
                .orElseThrow(() -> {
                    log.warn("Purpose not found: code={} in organization={}", purposeCode, organizationCode);
                    return new IllegalArgumentException("Purpose not found with code: " + purposeCode + " in organization: " + organizationCode);
                });
        return this.mapToResponse(purpose);
    }

    /**
     * POST /api/organizations/v1/{organizationCode}/purpose/
     * Create a purpose for the given organization.
     */
    @Transactional
    public PurposeResponse createForOrganization(String organizationCode, PurposeRequest request) {
        log.debug("Creating purpose for organization: code={}, name={}",
                organizationCode, request != null ? request.getName() : null);
        if (request == null) {
            throw new IllegalArgumentException("Purpose request body is required");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Purpose name is required");
        }

        Organization organization = getOrganizationOrThrow(organizationCode);

        String description = (request.getDescription() != null && !request.getDescription().trim().isEmpty())
                ? request.getDescription().trim()
                : null;

        Purpose purpose = Purpose.builder()
                .organizationId(organization.getId())
                .name(request.getName().trim())
                .description(description)
                .expireAt(request.getExpireAt())
                .build();

        purpose = purposeRepository.save(purpose);
        log.info("Purpose created: code={}, organizationCode={}", purpose.getCode(), organizationCode);
        return this.mapToResponse(purpose);
    }

    /**
     * PATCH/PUT /api/organizations/v1/{organizationCode}/purpose/{purposeCode}
     * Update a particular purpose.
     */
    @Transactional
    public PurposeResponse updateForOrganization(String organizationCode, String purposeCode, PurposeRequest request) {
        log.debug("Updating purpose: purposeCode={}, organizationCode={}", purposeCode, organizationCode);
        if (purposeCode == null || purposeCode.isBlank()) {
            throw new IllegalArgumentException("Purpose code is required for update");
        }
        Organization organization = getOrganizationOrThrow(organizationCode);
        Purpose purpose = purposeRepository.findByCodeAndOrganizationId(purposeCode, organization.getId())
                .orElseThrow(() -> {
                    log.warn("Purpose not found for update: code={} in organization={}", purposeCode, organizationCode);
                    return new IllegalArgumentException("Purpose not found with code: " + purposeCode + " in organization: " + organizationCode);
                });

        if (request != null) {
            if (request.getName() != null && !request.getName().isBlank()) {
                purpose.setName(request.getName().trim());
            }
            if (request.getDescription() != null) {
                purpose.setDescription(!request.getDescription().trim().isEmpty()
                        ? request.getDescription().trim()
                        : null);
            }
            if (request.getExpireAt() != null) {
                purpose.setExpireAt(request.getExpireAt());
            }
        }

        purpose = purposeRepository.save(purpose);
        log.info("Purpose updated: code={}, organizationCode={}", purpose.getCode(), organizationCode);
        return this.mapToResponse(purpose);
    }

    /**
     * DELETE /api/organizations/v1/{organizationCode}/purpose/{purposeCode}
     * Delete a particular purpose.
     */
    @Transactional
    public void deleteForOrganization(String organizationCode, String purposeCode) {
        log.debug("Deleting purpose: purposeCode={}, organizationCode={}", purposeCode, organizationCode);
        if (purposeCode == null || purposeCode.isBlank()) {
            throw new IllegalArgumentException("Purpose code is required for deletion");
        }
        Organization organization = getOrganizationOrThrow(organizationCode);
        Purpose purpose = purposeRepository.findByCodeAndOrganizationId(purposeCode, organization.getId())
                .orElseThrow(() -> {
                    log.warn("Purpose not found for deletion: code={} in organization={}", purposeCode, organizationCode);
                    return new IllegalArgumentException("Purpose not found with code: " + purposeCode + " in organization: " + organizationCode);
                });

        purposeRepository.delete(purpose);
        log.info("Purpose deleted: code={}, organizationCode={}", purposeCode, organizationCode);
    }

    public java.util.Optional<Purpose> findPurposeByIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return java.util.Optional.empty();
        }
        java.util.Optional<Purpose> byCode = purposeRepository.findByCode(identifier.trim());
        if (byCode.isPresent()) {
            return byCode;
        }
        try {
            Long id = Long.parseLong(identifier.trim());
            return purposeRepository.findById(id);
        } catch (NumberFormatException e) {
            return java.util.Optional.empty();
        }
    }

    public boolean validatePurpose(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return false;
        }
        java.util.Optional<Purpose> opt = findPurposeByIdentifier(identifier.trim());
        if (opt.isEmpty()) {
            return false;
        }
        Purpose purpose = opt.get();
        if (purpose.getExpireAt() != null && purpose.getExpireAt().isBefore(java.time.OffsetDateTime.now())) {
            return false;
        }
        return true;
    }

    private Organization getOrganizationOrThrow(String organizationCode) {
        if (organizationCode == null || organizationCode.isBlank()) {
            throw new IllegalArgumentException("Organization code is required");
        }
        return organizationRepository.findByCode(organizationCode)
                .orElseThrow(() -> {
                    log.warn("Organization not found: code={}", organizationCode);
                    return new IllegalArgumentException("Organization not found with code: " + organizationCode);
                });
    }

    private PurposeResponse mapToResponse(Purpose purpose) {
        return PurposeResponse.builder()
                .code(purpose.getCode())
                .name(purpose.getName())
                .description(purpose.getDescription())
                .createdAt(purpose.getCreatedAt())
                .expireAt(purpose.getExpireAt())
                .build();
    }
}

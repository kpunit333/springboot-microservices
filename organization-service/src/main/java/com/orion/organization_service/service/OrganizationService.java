package com.orion.organization_service.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orion.organization_service.dto.OrganizationInternalDto;
import com.orion.organization_service.dto.OrganizationRequest;
import com.orion.organization_service.dto.OrganizationResponse;
import com.orion.organization_service.model.Organization;
import com.orion.organization_service.repository.OrganizationRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OrganizationService {

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<OrganizationResponse> getAll() {
        log.debug("Fetching all organizations");
        List<Organization> list = organizationRepository.findAll();
        log.debug("Found {} organizations", list.size());
        return list.stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public OrganizationResponse create(OrganizationRequest request) {
        log.debug("Persisting new organization: name={}", request.getName());
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Organization name is required");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
        if (organizationRepository.existsByName(request.getName().trim())) {
            log.warn("Organization already exists with name: {}", request.getName());
            throw new IllegalArgumentException("Organization already exists with name: " + request.getName());
        }
        Organization organization = Organization.builder()
                .name(request.getName().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        organization = organizationRepository.save(organization);
        log.info("Organization saved: id={}, code={}", organization.getId(), organization.getCode());
        return this.mapToResponse(organization);
    }

    @Transactional
    public OrganizationResponse update(OrganizationRequest request) {
        log.debug("Updating organization: code={}", request.getCode());
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new IllegalArgumentException("Organization code is required for update");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required for update");
        }

        Organization organization = organizationRepository.findByCode(request.getCode())
                .orElseThrow(() -> {
                    log.warn("Update failed — organization not found: code={}", request.getCode());
                    return new IllegalArgumentException("Organization not found with code: " + request.getCode());
                });

        if (!passwordEncoder.matches(request.getPassword(), organization.getPassword())) {
            log.warn("Update failed — invalid credentials for organization code: {}", request.getCode());
            throw new IllegalArgumentException("Invalid organization credentials");
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            organization.setName(request.getName().trim());
        }
        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            organization.setPassword(passwordEncoder.encode(request.getNewPassword()));
            log.debug("Password updated for organization: code={}", request.getCode());
        }

        organization = organizationRepository.save(organization);
        log.info("Organization updated: code={}", organization.getCode());
        return this.mapToResponse(organization);
    }

    public OrganizationResponse getByCode(String code) {
        log.debug("Looking up organization: code={}", code);
        Organization organization = organizationRepository.findByCode(code)
                .orElseThrow(() -> {
                    log.warn("Organization not found: code={}", code);
                    return new IllegalArgumentException("Organization not found with code: " + code);
                });
        return this.mapToResponse(organization);
    }

    public OrganizationInternalDto getByCodeForAuth(String code) {
        log.debug("Auth lookup for organization: code={}", code);
        Organization organization = organizationRepository.findByCode(code)
                .orElseThrow(() -> {
                    log.warn("Auth lookup failed — organization not found: code={}", code);
                    return new IllegalArgumentException("Organization not found with code: " + code);
                });
        return toInternalDto(organization);
    }

    public OrganizationInternalDto getByNameForAuth(String name) {
        log.debug("Auth lookup for organization: name={}", name);
        Organization organization = organizationRepository.findByNameIgnoreCase(name.trim())
                .or(() -> organizationRepository.findByName(name.trim()))
                .orElseThrow(() -> {
                    log.warn("Auth lookup failed — organization not found: name={}", name);
                    return new IllegalArgumentException("Organization not found with name: " + name);
                });
        return toInternalDto(organization);
    }

    public OrganizationInternalDto getByIdentifierForAuth(String identifier) {
        log.debug("Auth lookup for organization: identifier={}", identifier);
        Organization organization = organizationRepository.findByCode(identifier.trim())
                .or(() -> organizationRepository.findByNameIgnoreCase(identifier.trim()))
                .or(() -> organizationRepository.findByName(identifier.trim()))
                .orElseThrow(() -> {
                    log.warn("Auth lookup failed — organization not found with code or name: {}", identifier);
                    return new IllegalArgumentException("Organization not found with code or name: " + identifier);
                });
        return toInternalDto(organization);
    }

    private OrganizationInternalDto toInternalDto(Organization organization) {
        return OrganizationInternalDto.builder()
                .id(organization.getId())
                .name(organization.getName())
                .code(organization.getCode())
                .password(organization.getPassword())
                .status(organization.getStatus())
                .expiryAt(organization.getExpiryAt())
                .build();
    }

    @Transactional
    public void deleteByCode(String code) {
        log.debug("Deleting organization: code={}", code);
        organizationRepository.findByCode(code)
                .orElseThrow(() -> {
                    log.warn("Delete failed — organization not found: code={}", code);
                    return new IllegalArgumentException("Organization not found with code: " + code);
                });
        organizationRepository.deleteByCode(code);
        log.info("Organization deleted: code={}", code);
    }

    public OrganizationResponse mapToResponse(Organization organization) {
        return OrganizationResponse.builder()
                .name(organization.getName())
                .code(organization.getCode())
                .status(organization.getStatus())
                .createdAt(organization.getCreatedAt())
                .build();
    }
}

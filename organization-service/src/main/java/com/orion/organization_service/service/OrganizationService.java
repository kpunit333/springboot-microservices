package com.orion.organization_service.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orion.organization_service.constant.OrganizationStatus;
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
        log.debug("Persisting new organization: name={}, email={}", request.getName(), request.getEmail());
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Organization name is required");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
        if (organizationRepository.existsByName(request.getName().trim())) {
            log.warn("Organization already exists with name: {}", request.getName());
            throw new IllegalArgumentException("Organization already exists with name: " + request.getName());
        }
        if (organizationRepository.existsByEmail(request.getEmail().trim())) {
            log.warn("Organization already exists with email: {}", request.getEmail());
            throw new IllegalArgumentException("Organization already exists with email: " + request.getEmail());
        }

        Organization organization = Organization.builder()
                .name(request.getName().trim())
                .email(request.getEmail().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .expireAt(request.getExpireAt())
                .status(request.getStatus() != null && !request.getStatus().isBlank() 
                        ? request.getStatus().trim() 
                        : OrganizationStatus.ACTIVE)
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

        Organization organization = organizationRepository.findByCode(request.getCode().trim())
                .orElseThrow(() -> {
                    log.warn("Update failed — organization not found: code={}", request.getCode());
                    return new IllegalArgumentException("Organization not found with code: " + request.getCode());
                });

        if (!passwordEncoder.matches(request.getPassword(), organization.getPassword())) {
            log.warn("Update failed — invalid credentials for organization code: {}", request.getCode());
            throw new IllegalArgumentException("Invalid organization credentials");
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            String newName = request.getName().trim();
            if (!newName.equalsIgnoreCase(organization.getName())) {
                if (organizationRepository.existsByName(newName)) {
                    log.warn("Organization update failed — name already in use: {}", newName);
                    throw new IllegalArgumentException("Organization already exists with name: " + newName);
                }
                organization.setName(newName);
            }
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String newEmail = request.getEmail().trim();
            if (!newEmail.equalsIgnoreCase(organization.getEmail())) {
                if (organizationRepository.existsByEmail(newEmail)) {
                    log.warn("Organization update failed — email already in use: {}", newEmail);
                    throw new IllegalArgumentException("Organization already exists with email: " + newEmail);
                }
                organization.setEmail(newEmail);
            }
        }
        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            organization.setPassword(passwordEncoder.encode(request.getNewPassword()));
            log.debug("Password updated for organization: code={}", request.getCode());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            organization.setStatus(request.getStatus().trim());
        }
        if (request.getExpireAt() != null) {
            organization.setExpireAt(request.getExpireAt());
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

    public OrganizationInternalDto getByEmailForAuth(String email) {
        log.debug("Auth lookup for organization: email={}", email);
        Organization organization = organizationRepository.findByEmailIgnoreCase(email.trim())
                .or(() -> organizationRepository.findByEmail(email.trim()))
                .orElseThrow(() -> {
                    log.warn("Auth lookup failed — organization not found: email={}", email);
                    return new IllegalArgumentException("Organization not found with email: " + email);
                });
        return toInternalDto(organization);
    }

    public OrganizationInternalDto getByIdentifierForAuth(String identifier) {
        log.debug("Auth lookup for organization: identifier={}", identifier);
        Organization organization = organizationRepository.findByCode(identifier.trim())
                .or(() -> organizationRepository.findByEmailIgnoreCase(identifier.trim()))
                .or(() -> organizationRepository.findByEmail(identifier.trim()))
                .orElseThrow(() -> {
                    log.warn("Auth lookup failed — organization not found with code or email: {}", identifier);
                    return new IllegalArgumentException("Organization not found with code or email: " + identifier);
                });
        return toInternalDto(organization);
    }

    private OrganizationInternalDto toInternalDto(Organization organization) {
        return OrganizationInternalDto.builder()
                .id(organization.getId())
                .name(organization.getName())
                .code(organization.getCode())
                .email(organization.getEmail())
                .password(organization.getPassword())
                .status(organization.getStatus())
                .createdAt(organization.getCreatedAt())
                .expireAt(organization.getExpireAt())
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
                .email(organization.getEmail())
                .status(organization.getStatus())
                .createdAt(organization.getCreatedAt())
                .expireAt(organization.getExpireAt())
                .build();
    }
}

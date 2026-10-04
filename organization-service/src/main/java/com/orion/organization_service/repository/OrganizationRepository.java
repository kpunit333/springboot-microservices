package com.orion.organization_service.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.orion.organization_service.model.Organization;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    Optional<Organization> findByCode(String code);

    Optional<Organization> findByName(String name);

    Optional<Organization> findByNameIgnoreCase(String name);

    boolean existsByName(String name);

    boolean existsByCode(String code);

    void deleteByCode(String code);
}

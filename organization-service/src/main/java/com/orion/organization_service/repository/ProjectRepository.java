package com.orion.organization_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.orion.organization_service.model.Project;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByCode(String code);

    boolean existsByCode(String code);

    void deleteByCode(String code);

    List<Project> findByOrganizationId(Long organizationId);

    Optional<Project> findByCodeAndOrganizationId(String code, Long organizationId);

    boolean existsByCodeAndOrganizationId(String code, Long organizationId);

    void deleteByCodeAndOrganizationId(String code, Long organizationId);
}

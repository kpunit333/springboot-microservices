package com.orion.organization_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.orion.organization_service.model.Purpose;

@Repository
public interface PurposeRepository extends JpaRepository<Purpose, Long> {

    Optional<Purpose> findByCode(String code);

    boolean existsByCode(String code);

    void deleteByCode(String code);

    List<Purpose> findByOrganizationId(Long organizationId);

    Optional<Purpose> findByCodeAndOrganizationId(String code, Long organizationId);

    boolean existsByCodeAndOrganizationId(String code, Long organizationId);

    void deleteByCodeAndOrganizationId(String code, Long organizationId);
}

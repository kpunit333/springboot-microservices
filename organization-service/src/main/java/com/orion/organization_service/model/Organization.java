package com.orion.organization_service.model;

import java.time.LocalDateTime;

import com.orion.organization_service.constant.OrganizationStatus;
import com.orion.organization_service.util.CodeGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "organization", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organization {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "organization_id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    // Updatable is false so the code cannot be altered after generation
    @Column(name = "code", length = 18, nullable = false, unique = true, updatable = false)
    private String code;

    @Column(name = "password", length = 100, nullable = false)
    private String password;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "expiry_at")
    private LocalDateTime expiryAt;

    @Column(name = "status", columnDefinition = "text")
    @Builder.Default
    private String status = OrganizationStatus.ACTIVE;
    
    @PrePersist
    public void generateCode() {
        if (this.code == null || this.code.trim().isEmpty()) {
            this.code = CodeGenerator.generateOrganizationCode();
        }
    }
}

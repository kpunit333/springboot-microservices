package com.orion.organization_service.model;

import java.time.OffsetDateTime;
import java.util.UUID;

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
@Table(name = "project", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_id", nullable = false, updatable = false)
    private Long id;

    /** Reference to the owning organisation (organization_id BIGINT). */
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "code", length = 18, unique = true, nullable = false)
    private String code;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    /** Auto-generated UUID used as a unique secret key for the project. */
    @Column(name = "secret_key", unique = true, nullable = false, updatable = false,
            columnDefinition = "uuid")
    private UUID secretKey;

    @Column(name = "status", columnDefinition = "text")
    @Builder.Default
    private String status = "Active";

    // TIMESTAMP WITH TIME ZONE -> OffsetDateTime
    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    // TIMESTAMP WITH TIME ZONE -> OffsetDateTime (nullable)
    @Column(name = "expire_at")
    private OffsetDateTime expireAt;

    @Column(name = "description", length = 100)
    private String description;

    @PrePersist
    public void prePersist() {
        if (this.secretKey == null) {
            this.secretKey = UUID.randomUUID();
        }
        if (this.code == null || this.code.trim().isEmpty()) {
            this.code = CodeGenerator.generateProjectCode();
        }
    }
}

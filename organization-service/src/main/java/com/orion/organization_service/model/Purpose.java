package com.orion.organization_service.model;

import java.time.OffsetDateTime;

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
@Table(name = "purpose", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Purpose {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "purpose_id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "code", length = 18, unique = true, nullable = false, updatable = false)
    private String code;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "description", length = 100)
    private String description;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "expire_at")
    private OffsetDateTime expireAt;

    @PrePersist
    public void prePersist() {
        if (this.code == null || this.code.trim().isEmpty()) {
            this.code = CodeGenerator.generatePurposeCode();
        }
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
    }
}

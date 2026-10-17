package com.orion.user_service.model;

import java.time.OffsetDateTime;

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
@Table(name = "users", schema = "public")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Builder.Default
    @Column(name = "status", nullable = false)
    private String status = "ACTIVE";

    @Builder.Default
    @Column(name = "unlock_attempts", nullable = false)
    private Short unlockAttempts = 0;

    // timestamptz -> OffsetDateTime
    @Column(name = "unlock_at")
    private OffsetDateTime unlockAt;

    // timestamptz -> OffsetDateTime
    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    // timestamptz -> OffsetDateTime
    @Column(name = "expire_at")
    private OffsetDateTime expireAt;

    @Column(name = "role", columnDefinition = "text")
    private String role;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
        if (this.unlockAttempts == null) {
            this.unlockAttempts = 0;
        }
        if (this.status == null || this.status.isBlank()) {
            this.status = "ACTIVE";
        }
    }
}
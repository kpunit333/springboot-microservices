package com.orion.user_service.model;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
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

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "status", nullable = false)
    private String status;

    @Builder.Default
    @Column(name = "unlock_attempts", nullable = false)
    private Short unlockAttempts = 0;

    // timestamp without time zone -> LocalDateTime
    @Column(name = "unlock_at")
    private LocalDateTime unlockAt;

    // timestamp with time zone -> OffsetDateTime
    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    // timestamp without time zone -> LocalDateTime
    @Column(name = "expire_at")
    private LocalDateTime expireAt;

    @Column(name = "role", columnDefinition = "text")
    private String role;
}
package com.orion.user_service.dto;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInternalDto {

    private Long userId;

    private String username;

    private String name;

    private String password;

    private String status;

    private Short unlockAttempts;

    private LocalDateTime unlockAt;

    private OffsetDateTime createdAt;

    private LocalDateTime expireAt;

    private String role;
}

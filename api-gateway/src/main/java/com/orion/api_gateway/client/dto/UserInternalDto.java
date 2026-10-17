package com.orion.api_gateway.client.dto;

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

    private String email;

    private String password;

    private String status;

    private Short unlockAttempts;

    private OffsetDateTime unlockAt;

    private OffsetDateTime createdAt;

    private OffsetDateTime expireAt;

    private String role;
}

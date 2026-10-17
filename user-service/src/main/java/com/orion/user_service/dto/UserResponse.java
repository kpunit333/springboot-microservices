package com.orion.user_service.dto;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private String username;

    private String name;

    private String email;

    private String status;

    private String role;

    private OffsetDateTime createdAt;

    private OffsetDateTime expireAt;
}
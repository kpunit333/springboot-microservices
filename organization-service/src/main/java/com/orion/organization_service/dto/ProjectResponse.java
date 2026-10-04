package com.orion.organization_service.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectResponse {

    private String code;

    private String name;

    private UUID secretKey;

    private String status;

    private String description;

    private OffsetDateTime createdAt;
}

package com.orion.organization_service.dto;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

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

    private String email;

    private OffsetDateTime createdAt;

    private OffsetDateTime expireAt;

    @JsonProperty("channels")
    @Builder.Default
    private List<String> channels = new ArrayList<>();
}

package com.orion.organization_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResponse {

    private boolean valid;

    private String message;

    private Long organizationId;

    private String secretKey;

    private String projectName;

    private String purposeName;

    private java.util.List<String> channels;
}


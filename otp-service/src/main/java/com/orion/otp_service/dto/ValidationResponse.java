package com.orion.otp_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ValidationResponse {

    private boolean valid;

    private String message;

    private Long organizationId;

    private String secretKey;

    @com.fasterxml.jackson.annotation.JsonProperty("projectName")
    @com.fasterxml.jackson.annotation.JsonAlias({ "projectName", "project_name" })
    private String projectName;

    @com.fasterxml.jackson.annotation.JsonProperty("purposeName")
    @com.fasterxml.jackson.annotation.JsonAlias({ "purposeName", "purpose_name", "purpose" })
    private String purposeName;

    @com.fasterxml.jackson.annotation.JsonProperty("channels")
    @com.fasterxml.jackson.annotation.JsonAlias({ "channels", "channel" })
    private java.util.List<String> channels;
}


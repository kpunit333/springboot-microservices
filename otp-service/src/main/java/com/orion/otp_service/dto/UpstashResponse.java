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
public class UpstashResponse {

    private Object result;

    private String error;

    public String getResultAsString() {
        return result != null ? result.toString() : null;
    }
}

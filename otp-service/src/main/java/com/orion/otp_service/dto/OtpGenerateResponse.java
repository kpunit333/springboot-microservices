package com.orion.otp_service.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OtpGenerateResponse {

    private String secretKey;

    private String purposeId;

    private OtpUserDto user;

    private String otp;

    private long expiresInSeconds;

    @JsonProperty("channels")
    private List<String> channels;
}

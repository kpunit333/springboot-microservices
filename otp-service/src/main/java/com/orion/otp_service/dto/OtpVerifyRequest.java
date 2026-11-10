package com.orion.otp_service.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
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
public class OtpVerifyRequest {

    /**
     * Secret key from the project table.
     */
    @JsonProperty("secretKey")
    @JsonAlias({ "secretKey", "secret_key" })
    private String secretKey;

    @JsonProperty("user")
    private OtpUserDto user;

    @JsonProperty("otp")
    @JsonAlias({ "otp", "code" })
    private String otp;
}

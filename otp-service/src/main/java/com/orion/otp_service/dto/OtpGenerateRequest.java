package com.orion.otp_service.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
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
public class OtpGenerateRequest {

    /**
     * Secret key from the project table.
     */
    @JsonProperty("secretKey")
    @JsonAlias({ "secretKey", "secret_key" })
    private String secretKey;

    @JsonProperty("purposeId")
    @JsonAlias({ "purposeId", "purpose_id" })
    private String purposeId;

    @JsonProperty("user")
    private OtpUserDto user;

    /**
     * Optional channels specified in the request (e.g. for /test/generate).
     */
    @JsonProperty("channels")
    @JsonAlias({ "channels", "channel" })
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<String> channels;

    /**
     * Optional dispatch mode override: "WAY_1", "WAY_2", or "BOTH".
     * If not provided, defaults to notification.service.dispatch-mode configuration.
     */
    @JsonProperty("dispatchMode")
    @JsonAlias({ "dispatchMode", "dispatch_mode", "way" })
    private String dispatchMode;
}

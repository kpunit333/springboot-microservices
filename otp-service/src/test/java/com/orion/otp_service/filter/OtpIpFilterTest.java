package com.orion.otp_service.filter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.orion.otp_service.config.OtpIpProperties;
import com.orion.otp_service.controller.OtpV1TestController;
import com.orion.otp_service.controller.OtpV1Controller;
import com.orion.otp_service.dto.OtpGenerateRequest;
import com.orion.otp_service.dto.OtpGenerateResponse;
import com.orion.otp_service.dto.OtpUserDto;
import com.orion.otp_service.dto.OtpVerifyRequest;
import com.orion.otp_service.exception.GlobalExceptionHandler;
import com.orion.otp_service.service.OtpService;

class OtpIpFilterTest {

    private MockMvc mockMvc;
    private OtpService otpService;
    private OtpIpProperties properties;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        otpService = mock(OtpService.class);
        properties = new OtpIpProperties();
        properties.setEnabled(true);
        properties.setIps(List.of("127.0.0.1", "::1", "0:0:0:0:0:0:0:1", "localhost", "10.0.0.0/16"));

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        OtpIpFilter filter = new OtpIpFilter(properties, objectMapper);
        OtpV1Controller v1Controller = new OtpV1Controller(otpService);
        OtpV1TestController testController = new OtpV1TestController(otpService);

        mockMvc = MockMvcBuilders.standaloneSetup(v1Controller, testController)
                .addFilters(filter)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private OtpGenerateRequest sampleGenerateRequest() {
        return OtpGenerateRequest.builder()
                .secretKey("secret-123")
                .purposeId("PUR001")
                .user(OtpUserDto.builder()
                        .id("USR001")
                        .email("user@example.com")
                        .phone("9876543210")
                        .countryCode("+1")
                        .whatsapp("+19876543210")
                        .build())
                .build();
    }

    private OtpVerifyRequest sampleVerifyRequest() {
        return OtpVerifyRequest.builder()
                .secretKey("secret-123")
                .user(OtpUserDto.builder().id("USR001").build())
                .otp("123456")
                .build();
    }

    private OtpGenerateResponse sampleGenerateResponse() {
        return OtpGenerateResponse.builder()
                .secretKey("secret-123")
                .purposeId("PUR001")
                .user(OtpUserDto.builder().id("USR001").build())
                .otp("123456")
                .expiresInSeconds(300)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // /api/otp/v1/generate (Restricted)
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    void generateEndpoint_AllowedIp_ReturnsSuccess() throws Exception {
        when(otpService.generateOtp(any())).thenReturn(sampleGenerateResponse());

        mockMvc.perform(post("/api/otp/v1/generate")
                .with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleGenerateRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.otp").value("123456"));
    }

    @Test
    void generateEndpoint_AllowedIp_WithNewPayload_ReturnsSuccess() throws Exception {
        when(otpService.generateOtp(any())).thenReturn(sampleGenerateResponse());

        String newPayload = """
                {
                    "secretKey": "secret-123",
                    "purposeId": "LOGIN",
                    "user": {
                        "id": "USR001",
                        "email": "user@example.com",
                        "phone": "9876543210",
                        "countryCode": "+1",
                        "whatsapp": "+19876543210"
                    }
                }
                """;

        mockMvc.perform(post("/api/otp/v1/generate")
                .with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(newPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void generateEndpoint_DisallowedIp_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/otp/v1/generate")
                .with(request -> {
                    request.setRemoteAddr("198.51.100.5");
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleGenerateRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Access denied: IP [198.51.100.5] is not allowed to access this endpoint"));
    }

    @Test
    void generateEndpoint_DisallowedXForwardedForIp_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/otp/v1/generate")
                .with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                })
                .header("X-Forwarded-For", "203.0.113.195, 127.0.0.1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleGenerateRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Access denied: IP [203.0.113.195] is not allowed to access this endpoint"));
    }

    @Test
    void generateEndpoint_AllowedXForwardedForIp_ReturnsSuccess() throws Exception {
        when(otpService.generateOtp(any())).thenReturn(sampleGenerateResponse());

        mockMvc.perform(post("/api/otp/v1/generate")
                .with(request -> {
                    request.setRemoteAddr("198.51.100.5");
                    return request;
                })
                .header("X-Forwarded-For", "127.0.0.1, 10.0.0.1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleGenerateRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void generateEndpoint_AllowedCidrSubnetIp_ReturnsSuccess() throws Exception {
        when(otpService.generateOtp(any())).thenReturn(sampleGenerateResponse());

        mockMvc.perform(post("/api/otp/v1/generate")
                .with(request -> {
                    request.setRemoteAddr("10.0.5.23");
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleGenerateRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // /api/otp/v1/verify (Restricted)
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    void verifyEndpoint_AllowedIp_ReturnsSuccess() throws Exception {
        doNothing().when(otpService).verifyOtp(any());

        mockMvc.perform(post("/api/otp/v1/verify")
                .with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleVerifyRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void verifyEndpoint_AllowedIp_WithNewPayload_ReturnsSuccess() throws Exception {
        doNothing().when(otpService).verifyOtp(any());

        String newPayload = """
                {
                    "secretKey": "secret-123",
                    "user": {
                        "id": "USR001"
                    },
                    "otp": "123456"
                }
                """;

        mockMvc.perform(post("/api/otp/v1/verify")
                .with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(newPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void verifyEndpoint_DisallowedIp_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/otp/v1/verify")
                .with(request -> {
                    request.setRemoteAddr("198.51.100.5");
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleVerifyRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Access denied: IP [198.51.100.5] is not allowed to access this endpoint"));
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // /api/otp/v1/test/generate and /test/generate (Unrestricted)
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    void apiTestGenerateEndpoint_DisallowedIp_StillAllowed() throws Exception {
        when(otpService.generateTestOtp(any())).thenReturn(sampleGenerateResponse());

        mockMvc.perform(post("/api/otp/v1/test/generate")
                .with(request -> {
                    request.setRemoteAddr("198.51.100.5"); // Disallowed IP
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleGenerateRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.otp").value("123456"));
    }

    @Test
    void rootTestGenerateEndpoint_DisallowedIp_StillAllowed() throws Exception {
        when(otpService.generateTestOtp(any())).thenReturn(sampleGenerateResponse());

        mockMvc.perform(post("/test/generate")
                .with(request -> {
                    request.setRemoteAddr("198.51.100.5"); // Disallowed IP
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleGenerateRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.otp").value("123456"));
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // /api/otp/v1/test/verify and /test/verify (Unrestricted)
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    void apiTestVerifyEndpoint_DisallowedIp_StillAllowed() throws Exception {
        doNothing().when(otpService).verifyOtp(any());

        mockMvc.perform(post("/api/otp/v1/test/verify")
                .with(request -> {
                    request.setRemoteAddr("198.51.100.5"); // Disallowed IP
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleVerifyRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void rootTestVerifyEndpoint_DisallowedIp_StillAllowed() throws Exception {
        doNothing().when(otpService).verifyOtp(any());

        mockMvc.perform(post("/test/verify")
                .with(request -> {
                    request.setRemoteAddr("198.51.100.5"); // Disallowed IP
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleVerifyRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // OPTIONS preflight and disabled filter bypass
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    void optionsPreflight_DisallowedIp_PassesThrough() throws Exception {
        mockMvc.perform(options("/api/otp/v1/generate")
                .with(request -> {
                    request.setRemoteAddr("198.51.100.5");
                    return request;
                }))
                .andExpect(status().isOk());
    }

    @Test
    void filterDisabled_DisallowedIp_PassesThrough() throws Exception {
        properties.setEnabled(false);
        when(otpService.generateOtp(any())).thenReturn(sampleGenerateResponse());

        mockMvc.perform(post("/api/otp/v1/generate")
                .with(request -> {
                    request.setRemoteAddr("198.51.100.5");
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleGenerateRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void filterDisabled_DisallowedIp_CanHitV1Generate() throws Exception {
        properties.setEnabled(false);
        when(otpService.generateOtp(any())).thenReturn(sampleGenerateResponse());

        mockMvc.perform(post("/v1/generate")
                .with(request -> {
                    request.setRemoteAddr("198.51.100.5");
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleGenerateRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void filterDisabled_DisallowedIp_CanHitV1Verify() throws Exception {
        properties.setEnabled(false);
        doNothing().when(otpService).verifyOtp(any());

        mockMvc.perform(post("/v1/verify")
                .with(request -> {
                    request.setRemoteAddr("198.51.100.5");
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleVerifyRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}

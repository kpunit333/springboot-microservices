package com.orion.otp_service.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orion.otp_service.dto.OtpGenerateRequest;
import com.orion.otp_service.dto.OtpGenerateResponse;
import com.orion.otp_service.dto.OtpUserDto;
import com.orion.otp_service.dto.OtpVerifyRequest;
import com.orion.otp_service.exception.GlobalExceptionHandler;
import com.orion.otp_service.service.OtpService;

class OtpControllerTest {

    private MockMvc mockMvc;
    private OtpService otpService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        otpService = mock(OtpService.class);
        OtpV1Controller otpController = new OtpV1Controller(otpService);
        OtpV1TestController testController = new OtpV1TestController(otpService);
        mockMvc = MockMvcBuilders.standaloneSetup(otpController, testController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testGenerateOtpEndpointSuccess() throws Exception {
        OtpUserDto user = OtpUserDto.builder()
                .id("USR202")
                .email("user@example.com")
                .phone("9876543210")
                .countryCode("+91")
                .whatsapp("+919876543210")
                .build();

        OtpGenerateResponse mockResponse = OtpGenerateResponse.builder()
                .secretKey("a65051f3-6e89-4c84-bf43-ca323dfbcbf2")
                .purposeId("PUR303")
                .user(user)
                .otp("543210")
                .expiresInSeconds(300)
                .channels(List.of("SMS", "WHATSAPP", "EMAIL"))
                .build();

        when(otpService.generateOtp(any(OtpGenerateRequest.class))).thenReturn(mockResponse);

        OtpGenerateRequest request = OtpGenerateRequest.builder()
                .secretKey("a65051f3-6e89-4c84-bf43-ca323dfbcbf2")
                .purposeId("PUR303")
                .user(user)
                .build();

        mockMvc.perform(post("/api/otp/v1/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("OTP generated successfully"))
                .andExpect(jsonPath("$.data.otp").value("543210"))
                .andExpect(jsonPath("$.data.secretKey").value("a65051f3-6e89-4c84-bf43-ca323dfbcbf2"))
                .andExpect(jsonPath("$.data.purposeId").value("PUR303"))
                .andExpect(jsonPath("$.data.user.id").value("USR202"))
                .andExpect(jsonPath("$.data.channels[0]").value("SMS"))
                .andExpect(jsonPath("$.data.channels[1]").value("WHATSAPP"))
                .andExpect(jsonPath("$.data.channels[2]").value("EMAIL"))
                .andExpect(jsonPath("$.data.expiresInSeconds").value(300));

        ArgumentCaptor<OtpGenerateRequest> captor = ArgumentCaptor.forClass(OtpGenerateRequest.class);
        verify(otpService).generateOtp(captor.capture());
        assertEquals("PUR303", captor.getValue().getPurposeId());
        assertEquals("USR202", captor.getValue().getUser().getId());
    }

    @Test
    void testGenerateOtpEndpoint_WithStrictPayloadFormat() throws Exception {
        OtpUserDto user = OtpUserDto.builder()
                .id("USR202")
                .email("test@example.com")
                .phone("9876543210")
                .countryCode("+91")
                .whatsapp("+919876543210")
                .build();

        OtpGenerateResponse mockResponse = OtpGenerateResponse.builder()
                .secretKey("a65051f3-6e89-4c84-bf43-ca323dfbcbf2")
                .purposeId("PUR303")
                .user(user)
                .otp("543210")
                .expiresInSeconds(300)
                .build();

        when(otpService.generateOtp(any(OtpGenerateRequest.class))).thenReturn(mockResponse);

        String jsonPayload = """
                {
                    "secretKey": "a65051f3-6e89-4c84-bf43-ca323dfbcbf2",
                    "purposeId": "PUR303",
                    "user": {
                        "id": "USR202",
                        "email": "test@example.com",
                        "phone": "9876543210",
                        "countryCode": "+91",
                        "whatsapp": "+919876543210"
                    }
                }
                """;

        mockMvc.perform(post("/api/otp/v1/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("OTP generated successfully"))
                .andExpect(jsonPath("$.data.secretKey").value("a65051f3-6e89-4c84-bf43-ca323dfbcbf2"))
                .andExpect(jsonPath("$.data.purposeId").value("PUR303"))
                .andExpect(jsonPath("$.data.user.id").value("USR202"))
                .andExpect(jsonPath("$.data.user.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.user.phone").value("9876543210"))
                .andExpect(jsonPath("$.data.user.countryCode").value("+91"))
                .andExpect(jsonPath("$.data.user.whatsapp").value("+919876543210"))
                .andExpect(jsonPath("$.data.otp").value("543210"));

        ArgumentCaptor<OtpGenerateRequest> captor = ArgumentCaptor.forClass(OtpGenerateRequest.class);
        verify(otpService).generateOtp(captor.capture());
        OtpGenerateRequest captured = captor.getValue();
        assertEquals("a65051f3-6e89-4c84-bf43-ca323dfbcbf2", captured.getSecretKey());
        assertEquals("PUR303", captured.getPurposeId());
        assertNotNull(captured.getUser());
        assertEquals("USR202", captured.getUser().getId());
        assertEquals("test@example.com", captured.getUser().getEmail());
        assertEquals("9876543210", captured.getUser().getPhone());
        assertEquals("+91", captured.getUser().getCountryCode());
        assertEquals("+919876543210", captured.getUser().getWhatsapp());
    }

    @Test
    void testApiTestGenerateOtpEndpoint_WithStrictPayloadFormat() throws Exception {
        OtpUserDto user = OtpUserDto.builder()
                .id("USR202")
                .build();

        OtpGenerateResponse mockResponse = OtpGenerateResponse.builder()
                .secretKey("a65051f3-6e89-4c84-bf43-ca323dfbcbf2")
                .purposeId("PUR303")
                .user(user)
                .otp("543210")
                .expiresInSeconds(300)
                .channels(List.of())
                .build();

        when(otpService.generateTestOtp(any(OtpGenerateRequest.class))).thenReturn(mockResponse);

        String jsonPayload = """
                {
                    "secretKey": "a65051f3-6e89-4c84-bf43-ca323dfbcbf2",
                    "purposeId": "PUR303",
                    "user": {
                        "id": "USR202"
                    }
                }
                """;

        mockMvc.perform(post("/api/otp/v1/test/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.purposeId").value("PUR303"))
                .andExpect(jsonPath("$.data.user.id").value("USR202"))
                .andExpect(jsonPath("$.data.channels").isEmpty());
    }

    @Test
    void testRootTestGenerateOtpEndpoint_WithStrictPayloadFormat() throws Exception {
        OtpUserDto user = OtpUserDto.builder()
                .id("USR202")
                .build();

        OtpGenerateResponse mockResponse = OtpGenerateResponse.builder()
                .secretKey("a65051f3-6e89-4c84-bf43-ca323dfbcbf2")
                .purposeId("PUR303")
                .user(user)
                .otp("543210")
                .expiresInSeconds(300)
                .channels(List.of())
                .build();

        when(otpService.generateTestOtp(any(OtpGenerateRequest.class))).thenReturn(mockResponse);

        String jsonPayload = """
                {
                    "secretKey": "a65051f3-6e89-4c84-bf43-ca323dfbcbf2",
                    "purposeId": "PUR303",
                    "user": {
                        "id": "USR202"
                    }
                }
                """;

        mockMvc.perform(post("/test/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.purposeId").value("PUR303"))
                .andExpect(jsonPath("$.data.user.id").value("USR202"))
                .andExpect(jsonPath("$.data.channels").isEmpty());
    }

    @Test
    void testTestGenerateOtpEndpoint_WithChannelsInRequest() throws Exception {
        OtpUserDto user = OtpUserDto.builder()
                .id("USR202")
                .email("user@example.com")
                .phone("9876543210")
                .build();

        OtpGenerateResponse mockResponse = OtpGenerateResponse.builder()
                .secretKey("a65051f3-6e89-4c84-bf43-ca323dfbcbf2")
                .purposeId("PUR303")
                .user(user)
                .otp("543210")
                .expiresInSeconds(300)
                .channels(List.of("EMAIL", "SMS"))
                .build();

        when(otpService.generateTestOtp(any(OtpGenerateRequest.class))).thenReturn(mockResponse);

        String jsonPayload = """
                {
                    "secretKey": "a65051f3-6e89-4c84-bf43-ca323dfbcbf2",
                    "purposeId": "PUR303",
                    "user": {
                        "id": "USR202",
                        "email": "user@example.com",
                        "phone": "9876543210"
                    },
                    "channels": ["EMAIL", "SMS"]
                }
                """;

        mockMvc.perform(post("/api/otp/v1/test/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.channels[0]").value("EMAIL"))
                .andExpect(jsonPath("$.data.channels[1]").value("SMS"));

        ArgumentCaptor<OtpGenerateRequest> captor = ArgumentCaptor.forClass(OtpGenerateRequest.class);
        verify(otpService).generateTestOtp(captor.capture());
        OtpGenerateRequest captured = captor.getValue();
        assertEquals(List.of("EMAIL", "SMS"), captured.getChannels());
    }

    @Test
    void testVerifyOtpEndpointSuccess() throws Exception {
        doNothing().when(otpService).verifyOtp(any(OtpVerifyRequest.class));

        OtpVerifyRequest request = OtpVerifyRequest.builder()
                .secretKey("a65051f3-6e89-4c84-bf43-ca323dfbcbf2")
                .user(OtpUserDto.builder().id("USR202").build())
                .otp("543210")
                .build();

        mockMvc.perform(post("/api/otp/v1/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("OTP verified successfully"));
    }

    @Test
    void testVerifyOtpEndpoint_WithStrictPayloadFormat() throws Exception {
        doNothing().when(otpService).verifyOtp(any(OtpVerifyRequest.class));

        String jsonPayload = """
                {
                    "secretKey": "a65051f3-6e89-4c84-bf43-ca323dfbcbf2",
                    "user": {
                        "id": "USR202"
                    },
                    "otp": "543210"
                }
                """;

        mockMvc.perform(post("/api/otp/v1/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("OTP verified successfully"));

        ArgumentCaptor<OtpVerifyRequest> captor = ArgumentCaptor.forClass(OtpVerifyRequest.class);
        verify(otpService).verifyOtp(captor.capture());
        OtpVerifyRequest captured = captor.getValue();
        assertEquals("a65051f3-6e89-4c84-bf43-ca323dfbcbf2", captured.getSecretKey());
        assertNotNull(captured.getUser());
        assertEquals("USR202", captured.getUser().getId());
        assertEquals("543210", captured.getOtp());
    }

    @Test
    void testVerifyOtpEndpointInvalidOtpReturnsBadRequest() throws Exception {
        doThrow(new IllegalArgumentException("Invalid OTP"))
                .when(otpService).verifyOtp(any(OtpVerifyRequest.class));

        OtpVerifyRequest request = OtpVerifyRequest.builder()
                .secretKey("a65051f3-6e89-4c84-bf43-ca323dfbcbf2")
                .user(OtpUserDto.builder().id("USR202").build())
                .otp("000000")
                .build();

        mockMvc.perform(post("/api/otp/v1/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid OTP"));
    }

    @Test
    void testDirectV1GenerateEndpointSuccess() throws Exception {
        OtpUserDto user = OtpUserDto.builder()
                .id("USR202")
                .email("user@example.com")
                .build();

        OtpGenerateResponse mockResponse = OtpGenerateResponse.builder()
                .secretKey("a65051f3-6e89-4c84-bf43-ca323dfbcbf2")
                .purposeId("PUR303")
                .user(user)
                .otp("543210")
                .expiresInSeconds(300)
                .channels(List.of("EMAIL"))
                .build();

        when(otpService.generateOtp(any(OtpGenerateRequest.class))).thenReturn(mockResponse);

        String jsonPayload = """
                {
                    "secretKey": "a65051f3-6e89-4c84-bf43-ca323dfbcbf2",
                    "purposeId": "PUR303",
                    "user": {
                        "id": "USR202",
                        "email": "user@example.com"
                    }
                }
                """;

        mockMvc.perform(post("/v1/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.otp").value("543210"));
    }

    @Test
    void testDirectV1VerifyEndpointSuccess() throws Exception {
        doNothing().when(otpService).verifyOtp(any(OtpVerifyRequest.class));

        String jsonPayload = """
                {
                    "secretKey": "a65051f3-6e89-4c84-bf43-ca323dfbcbf2",
                    "user": {
                        "id": "USR202"
                    },
                    "otp": "543210"
                }
                """;

        mockMvc.perform(post("/v1/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("OTP verified successfully"));
    }
}

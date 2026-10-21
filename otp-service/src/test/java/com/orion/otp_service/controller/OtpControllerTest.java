package com.orion.otp_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orion.otp_service.dto.OtpGenerateRequest;
import com.orion.otp_service.dto.OtpGenerateResponse;
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
                mockMvc = MockMvcBuilders.standaloneSetup(otpController)
                                .setControllerAdvice(new GlobalExceptionHandler())
                                .build();
        }

        @Test
        void testGenerateOtpEndpointSuccess() throws Exception {
                OtpGenerateResponse mockResponse = OtpGenerateResponse.builder()
                                .secretKey("a65051f3-6e89-4c84-bf43-ca323dfbcbf2")
                                .userId("USR202")
                                .purposeId("PUR303")
                                .otp("543210")
                                .expiresInSeconds(300)
                                .build();

                when(otpService.generateOtp(any(OtpGenerateRequest.class))).thenReturn(mockResponse);

                OtpGenerateRequest request = OtpGenerateRequest.builder()
                                .secretKey("a65051f3-6e89-4c84-bf43-ca323dfbcbf2")
                                .userId("USR202")
                                .purposeId("PUR303")
                                .build();

                mockMvc.perform(post("/api/otp/v1/generate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.message").value("OTP generated successfully"))
                                .andExpect(jsonPath("$.data.otp").value("543210"))
                                .andExpect(jsonPath("$.data.secretKey").value("a65051f3-6e89-4c84-bf43-ca323dfbcbf2"))
                                .andExpect(jsonPath("$.data.userId").value("USR202"))
                                .andExpect(jsonPath("$.data.expiresInSeconds").value(300));
        }

        @Test
        void testVerifyOtpEndpointSuccess() throws Exception {
                doNothing().when(otpService).verifyOtp(any(OtpVerifyRequest.class));

                OtpVerifyRequest request = OtpVerifyRequest.builder()
                                .secretKey("a65051f3-6e89-4c84-bf43-ca323dfbcbf2")
                                .userId("USR202")
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
        void testVerifyOtpEndpointInvalidOtpReturnsBadRequest() throws Exception {
                doThrow(new IllegalArgumentException("Invalid OTP"))
                                .when(otpService).verifyOtp(any(OtpVerifyRequest.class));

                OtpVerifyRequest request = OtpVerifyRequest.builder()
                                .secretKey("a65051f3-6e89-4c84-bf43-ca323dfbcbf2")
                                .userId("USR202")
                                .otp("000000")
                                .build();

                mockMvc.perform(post("/api/otp/v1/verify")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("Invalid OTP"));
        }
}

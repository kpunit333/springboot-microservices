package com.orion.otp_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.orion.otp_service.client.OrganizationServiceClient;
import com.orion.otp_service.config.OtpProperties;
import com.orion.otp_service.dto.OtpGenerateRequest;
import com.orion.otp_service.dto.OtpGenerateResponse;
import com.orion.otp_service.dto.OtpVerifyRequest;
import com.orion.otp_service.dto.ValidationResponse;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private UpstashRedisService redisService;

    @Mock
    private OrganizationServiceClient orgClient;

    @Mock
    private OtpProperties properties;

    private OtpService otpService;

    @BeforeEach
    void setUp() {
        otpService = new OtpService(redisService, orgClient, properties);
    }

    @Test
    void testGenerateOtpSuccess_WhenNoPriorOtp() {
        when(properties.getTtlSeconds()).thenReturn(300L);
        when(orgClient.validateProjectAndPurpose("SEC001", "PUR001")).thenReturn(
                ValidationResponse.builder().valid(true).organizationId(1L).secretKey("SEC001").message("Valid").build());
        when(redisService.exists("otp:SEC001:USR001")).thenReturn(false);

        OtpGenerateRequest request = OtpGenerateRequest.builder()
                .secretKey("SEC001")
                .userId("USR001")
                .purposeId("PUR001")
                .build();

        OtpGenerateResponse response = otpService.generateOtp(request);

        assertNotNull(response);
        assertEquals("SEC001", response.getSecretKey());
        assertEquals("USR001", response.getUserId());
        assertEquals("PUR001", response.getPurposeId());
        assertNotNull(response.getOtp());
        assertEquals(6, response.getOtp().length());
        assertEquals(300L, response.getExpiresInSeconds());

        // Verify it was stored with 5 min (300s) TTL
        verify(redisService).setWithTtl(eq("otp:SEC001:USR001"), eq(response.getOtp()), eq(300L));
        // Verify delete was not called since it did not exist
        verify(redisService, never()).delete("otp:SEC001:USR001");
    }

    @Test
    void testGenerateOtp_DeletesExistingOtpPriorToGenerating() {
        when(properties.getTtlSeconds()).thenReturn(300L);
        when(orgClient.validateProjectAndPurpose("SEC001", "PUR001")).thenReturn(
                ValidationResponse.builder().valid(true).organizationId(1L).secretKey("SEC001").message("Valid").build());
        when(redisService.exists("otp:SEC001:USR001")).thenReturn(true);

        OtpGenerateRequest request = OtpGenerateRequest.builder()
                .secretKey("SEC001")
                .userId("USR001")
                .purposeId("PUR001")
                .build();

        OtpGenerateResponse response = otpService.generateOtp(request);

        assertNotNull(response);
        // Verify delete was called because old OTP was present
        verify(redisService).delete("otp:SEC001:USR001");
        verify(redisService).setWithTtl(eq("otp:SEC001:USR001"), eq(response.getOtp()), eq(300L));
    }

    @Test
    void testGenerateOtp_InvalidProjectOrPurposeThrows() {
        when(orgClient.validateProjectAndPurpose("INVALID_SEC", "PUR001")).thenReturn(
                ValidationResponse.builder().valid(false).message("Project not found with secret key: INVALID_SEC").build());

        OtpGenerateRequest request = OtpGenerateRequest.builder()
                .secretKey("INVALID_SEC")
                .userId("USR001")
                .purposeId("PUR001")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> otpService.generateOtp(request));
        assertTrue(ex.getMessage().contains("Project not found with secret key"));
        verify(redisService, never()).setWithTtl(anyString(), anyString(), anyLong());
    }

    @Test
    void testGenerateOtp_DifferentOrganizationThrows() {
        when(orgClient.validateProjectAndPurpose("SEC001", "PUR002")).thenReturn(
                ValidationResponse.builder().valid(false).message("Project and purpose do not belong to the same organization").build());

        OtpGenerateRequest request = OtpGenerateRequest.builder()
                .secretKey("SEC001")
                .userId("USR001")
                .purposeId("PUR002")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> otpService.generateOtp(request));
        assertTrue(ex.getMessage().contains("do not belong to the same organization"));
        verify(redisService, never()).setWithTtl(anyString(), anyString(), anyLong());
    }

    @Test
    void testGenerateOtp_MissingFieldsThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> otpService.generateOtp(OtpGenerateRequest.builder().userId("U").purposeId("P").build()));
        assertThrows(IllegalArgumentException.class,
                () -> otpService.generateOtp(OtpGenerateRequest.builder().secretKey("SEC").purposeId("P").build()));
        assertThrows(IllegalArgumentException.class,
                () -> otpService.generateOtp(OtpGenerateRequest.builder().secretKey("SEC").userId("U").build()));
    }

    @Test
    void testVerifyOtpSuccess() {
        when(redisService.get("otp:SEC001:USR001")).thenReturn("123456");

        OtpVerifyRequest request = OtpVerifyRequest.builder()
                .secretKey("SEC001")
                .userId("USR001")
                .otp("123456")
                .build();

        otpService.verifyOtp(request);

        // Verify it was deleted on success
        verify(redisService).delete("otp:SEC001:USR001");
    }

    @Test
    void testVerifyOtp_MismatchThrows() {
        when(redisService.get("otp:SEC001:USR001")).thenReturn("123456");

        OtpVerifyRequest request = OtpVerifyRequest.builder()
                .secretKey("SEC001")
                .userId("USR001")
                .otp("654321")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> otpService.verifyOtp(request));
        assertEquals("Invalid OTP", ex.getMessage());
        verify(redisService, never()).delete("otp:SEC001:USR001");
    }

    @Test
    void testVerifyOtp_ExpiredOrNotFoundThrows() {
        when(redisService.get("otp:SEC001:USR001")).thenReturn(null);

        OtpVerifyRequest request = OtpVerifyRequest.builder()
                .secretKey("SEC001")
                .userId("USR001")
                .otp("123456")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> otpService.verifyOtp(request));
        assertTrue(ex.getMessage().contains("expired or does not exist"));
        verify(redisService, never()).delete("otp:SEC001:USR001");
    }
}

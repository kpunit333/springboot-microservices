package com.orion.otp_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.orion.otp_service.client.NotificationServiceClient;
import com.orion.otp_service.client.OrganizationServiceClient;
import com.orion.otp_service.config.NotificationProperties;
import com.orion.otp_service.config.OtpProperties;
import com.orion.otp_service.dto.OtpGenerateRequest;
import com.orion.otp_service.dto.OtpGenerateResponse;
import com.orion.otp_service.dto.OtpUserDto;
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

        @Mock
        private NotificationProducerService notificationService;

        @Mock
        private NotificationServiceClient notificationServiceClient;

        @Mock
        private NotificationProperties notificationProperties;

        private OtpService otpService;

        @BeforeEach
        void setUp() {
                otpService = new OtpService(redisService, orgClient, properties, notificationService);
        }

        @Test
        void testGenerateOtpSuccess_WhenNoPriorOtp() {
                when(properties.getTtlSeconds()).thenReturn(300L);
                when(orgClient.validateProjectAndPurpose("SEC001", "PUR001")).thenReturn(
                                ValidationResponse.builder()
                                                .valid(true)
                                                .organizationId(1L)
                                                .secretKey("SEC001")
                                                .message("Valid")
                                                .channels(List.of("SMS", "WHATSAPP", "EMAIL"))
                                                .build());
                when(redisService.exists("otp:SEC001:USR001")).thenReturn(false);
                when(notificationService.dispatchToChannels(anyList(), anyString(), any(OtpUserDto.class), anyString(),
                                anyString(), anyLong()))
                                .thenReturn(List.of("SMS", "WHATSAPP", "EMAIL"));

                OtpUserDto user = OtpUserDto.builder().id("USR001").build();
                OtpGenerateRequest request = OtpGenerateRequest.builder()
                                .secretKey("SEC001")
                                .purposeId("PUR001")
                                .user(user)
                                .build();

                OtpGenerateResponse response = otpService.generateOtp(request);

                assertNotNull(response);
                assertEquals("SEC001", response.getSecretKey());
                assertEquals("PUR001", response.getPurposeId());
                assertNotNull(response.getUser());
                assertEquals("USR001", response.getUser().getId());
                assertNotNull(response.getOtp());
                assertEquals(6, response.getOtp().length());
                assertEquals(300L, response.getExpiresInSeconds());
                assertEquals(List.of("SMS", "WHATSAPP", "EMAIL"), response.getChannels());

                // Verify it was stored with 5 min (300s) TTL
                verify(redisService).setWithTtl(eq("otp:SEC001:USR001"), eq(response.getOtp()), eq(300L));
                // Verify delete was not called since it did not exist
                verify(redisService, never()).delete("otp:SEC001:USR001");
                // Verify notification dispatch was triggered using project table channels
                verify(notificationService).dispatchToChannels(eq(List.of("SMS", "WHATSAPP", "EMAIL")), eq("SEC001"),
                                eq(user), eq("PUR001"), eq(response.getOtp()), eq(300L));
        }

        @Test
        void testGenerateOtp_WithStrictPayloadFormat() {
                when(properties.getTtlSeconds()).thenReturn(300L);
                when(orgClient.validateProjectAndPurpose("SEC001", "PUR_LOGIN")).thenReturn(
                                ValidationResponse.builder()
                                                .valid(true)
                                                .organizationId(1L)
                                                .secretKey("SEC001")
                                                .message("Valid")
                                                .channels(List.of("SMS", "EMAIL"))
                                                .build());
                when(redisService.exists("otp:SEC001:USR001")).thenReturn(false);
                when(notificationService.dispatchToChannels(anyList(), anyString(), any(OtpUserDto.class), anyString(),
                                anyString(), anyLong()))
                                .thenReturn(List.of("SMS", "EMAIL"));

                OtpUserDto user = OtpUserDto.builder()
                                .id("USR001")
                                .email("user@example.com")
                                .phone("9876543210")
                                .countryCode("+91")
                                .whatsapp("+919876543210")
                                .build();

                OtpGenerateRequest request = OtpGenerateRequest.builder()
                                .secretKey("SEC001")
                                .purposeId("PUR_LOGIN")
                                .user(user)
                                .build();

                OtpGenerateResponse response = otpService.generateOtp(request);

                assertNotNull(response);
                assertEquals("SEC001", response.getSecretKey());
                assertEquals("PUR_LOGIN", response.getPurposeId());
                assertNotNull(response.getUser());
                assertEquals("USR001", response.getUser().getId());
                assertEquals("user@example.com", response.getUser().getEmail());
                assertEquals("9876543210", response.getUser().getPhone());
                assertEquals("+91", response.getUser().getCountryCode());
                assertEquals("+919876543210", response.getUser().getWhatsapp());

                verify(redisService).setWithTtl(eq("otp:SEC001:USR001"), eq(response.getOtp()), eq(300L));
                OtpUserDto expectedDispatchedUser = OtpUserDto.builder()
                                .id("USR001")
                                .email("user@example.com")
                                .phone("9876543210")
                                .countryCode("+91")
                                .build();
                verify(notificationService).dispatchToChannels(eq(List.of("SMS", "EMAIL")), eq("SEC001"), eq(expectedDispatchedUser),
                                eq("PUR_LOGIN"), eq(response.getOtp()), eq(300L));
        }

        @Test
        void testGenerateOtp_UsesProjectChannels() {
                when(properties.getTtlSeconds()).thenReturn(300L);
                when(orgClient.validateProjectAndPurpose("SEC001", "PUR001")).thenReturn(
                                ValidationResponse.builder()
                                                .valid(true)
                                                .organizationId(1L)
                                                .secretKey("SEC001")
                                                .message("Valid")
                                                .channels(List.of("WHATSAPP"))
                                                .build());
                when(redisService.exists("otp:SEC001:USR001")).thenReturn(false);
                when(notificationService.dispatchToChannels(eq(List.of("WHATSAPP")), anyString(), any(OtpUserDto.class),
                                anyString(), anyString(), anyLong()))
                                .thenReturn(List.of("WHATSAPP"));

                OtpUserDto user = OtpUserDto.builder()
                                .id("USR001")
                                .email("user@example.com")
                                .whatsapp("+919876543210")
                                .build();
                OtpGenerateRequest request = OtpGenerateRequest.builder()
                                .secretKey("SEC001")
                                .purposeId("PUR001")
                                .user(user)
                                .build();

                OtpGenerateResponse response = otpService.generateOtp(request);

                assertNotNull(response);
                assertEquals(List.of("WHATSAPP"), response.getChannels());
                OtpUserDto expectedFilteredUser = OtpUserDto.builder()
                                .id("USR001")
                                .whatsapp("+919876543210")
                                .build();
                verify(notificationService).dispatchToChannels(eq(List.of("WHATSAPP")), eq("SEC001"), eq(expectedFilteredUser),
                                eq("PUR001"), eq(response.getOtp()), eq(300L));
        }

        @Test
        void testGenerateTestOtp_AllowsChannelsFromRequestWithoutDbCheck_AndDispatchesUser() {
                when(properties.getTtlSeconds()).thenReturn(300L);
                when(redisService.exists("otp:SEC001:USR001")).thenReturn(false);
                when(notificationService.dispatchToChannels(eq(List.of("EMAIL", "SMS")), anyString(), any(OtpUserDto.class),
                                anyString(), anyString(), anyLong()))
                                .thenReturn(List.of("EMAIL", "SMS"));

                OtpUserDto user = OtpUserDto.builder()
                                .id("USR001")
                                .email("user@example.com")
                                .phone("9876543210")
                                .whatsapp("+919876543210")
                                .build();
                OtpGenerateRequest request = OtpGenerateRequest.builder()
                                .secretKey("SEC001")
                                .purposeId("PUR001")
                                .user(user)
                                .channels(List.of("EMAIL", "SMS"))
                                .build();

                OtpGenerateResponse response = otpService.generateTestOtp(request);

                assertNotNull(response);
                assertEquals(List.of("EMAIL", "SMS"), response.getChannels());
                // DB validation is NOT called for test endpoint
                verify(orgClient, never()).validateProjectAndPurpose(anyString(), anyString());
                // Dispatches original unfiltered user
                verify(notificationService).dispatchToChannels(eq(List.of("EMAIL", "SMS")), eq("SEC001"), eq(user),
                                eq("PUR001"), eq(response.getOtp()), eq(300L));
        }

        @Test
        void testGenerateTestOtp_NoChannelsInRequest_DefaultsToEmptyAndNoDispatch() {
                when(properties.getTtlSeconds()).thenReturn(300L);
                when(redisService.exists("otp:SEC001:USR001")).thenReturn(false);

                OtpUserDto user = OtpUserDto.builder()
                                .id("USR001")
                                .email("user@example.com")
                                .phone("9876543210")
                                .build();
                // No channels in request
                OtpGenerateRequest request = OtpGenerateRequest.builder()
                                .secretKey("SEC001")
                                .purposeId("PUR001")
                                .user(user)
                                .build();

                OtpGenerateResponse response = otpService.generateTestOtp(request);

                assertNotNull(response);
                // channels should be empty — no dispatch when none provided
                assertNotNull(response.getChannels());
                assertTrue(response.getChannels().isEmpty());
                // DB validation is NOT called for test endpoint
                verify(orgClient, never()).validateProjectAndPurpose(anyString(), anyString());
                // No dispatch should happen (no channels)
                verify(notificationService, never()).dispatchToChannels(any(), anyString(), any(OtpUserDto.class),
                                anyString(), anyString(), anyLong());
        }

        @Test
        void testGenerateTestOtp_BlankOrWhitespaceChannelsInRequest_DefaultsToEmptyAndNoDispatch() {
                when(properties.getTtlSeconds()).thenReturn(300L);
                when(redisService.exists("otp:SEC001:USR001")).thenReturn(false);

                OtpUserDto user = OtpUserDto.builder()
                                .id("USR001")
                                .email("user@example.com")
                                .phone("9876543210")
                                .build();
                // Blank strings passed in channels list
                OtpGenerateRequest request = OtpGenerateRequest.builder()
                                .secretKey("SEC001")
                                .purposeId("PUR001")
                                .user(user)
                                .channels(List.of("", "   "))
                                .build();

                OtpGenerateResponse response = otpService.generateTestOtp(request);

                assertNotNull(response);
                assertNotNull(response.getChannels());
                assertTrue(response.getChannels().isEmpty());
                verify(orgClient, never()).validateProjectAndPurpose(anyString(), anyString());
                verify(notificationService, never()).dispatchToChannels(any(), anyString(), any(OtpUserDto.class),
                                anyString(), anyString(), anyLong());
        }

        @Test
        void testGenerateTestOtp_MixedBlankChannels_SendsOnlySpecifiedValidChannels() {
                when(properties.getTtlSeconds()).thenReturn(300L);
                when(redisService.exists("otp:SEC001:USR001")).thenReturn(false);
                when(notificationService.dispatchToChannels(eq(List.of("EMAIL")), anyString(), any(OtpUserDto.class),
                                anyString(), anyString(), anyLong()))
                                .thenReturn(List.of("EMAIL"));

                OtpUserDto user = OtpUserDto.builder()
                                .id("USR001")
                                .email("user@example.com")
                                .build();
                // Mixed blank and valid channel
                OtpGenerateRequest request = OtpGenerateRequest.builder()
                                .secretKey("SEC001")
                                .purposeId("PUR001")
                                .user(user)
                                .channels(List.of("  EMAIL  ", "", "   "))
                                .build();

                OtpGenerateResponse response = otpService.generateTestOtp(request);

                assertNotNull(response);
                assertEquals(List.of("EMAIL"), response.getChannels());
                verify(orgClient, never()).validateProjectAndPurpose(anyString(), anyString());
                verify(notificationService).dispatchToChannels(eq(List.of("EMAIL")), eq("SEC001"), eq(user),
                                eq("PUR001"), eq(response.getOtp()), eq(300L));
        }

        @Test
        void testGenerateOtp_OrgServiceThrowsException_ReturnsUserFriendlyError() {
                when(orgClient.validateProjectAndPurpose("SEC001", "PUR001"))
                                .thenThrow(new RuntimeException("Connection refused: org-service unreachable"));

                OtpUserDto user = OtpUserDto.builder().id("USR001").build();
                OtpGenerateRequest request = OtpGenerateRequest.builder()
                                .secretKey("SEC001")
                                .purposeId("PUR001")
                                .user(user)
                                .build();

                IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                () -> otpService.generateOtp(request));
                assertTrue(ex.getMessage().contains("Organization service is unavailable"),
                                "Expected user-friendly error but got: " + ex.getMessage());
                // OTP must NOT be stored
                verify(redisService, never()).setWithTtl(anyString(), anyString(), anyLong());
                // Notification must NOT be dispatched
                verify(notificationService, never()).dispatchToChannels(any(), anyString(), any(OtpUserDto.class),
                                anyString(), anyString(), anyLong());
        }

        @Test
        void testGenerateOtp_OrgServiceReturnsNull_ThrowsValidationError() {
                when(orgClient.validateProjectAndPurpose("SEC001", "PUR001")).thenReturn(null);

                OtpUserDto user = OtpUserDto.builder().id("USR001").build();
                OtpGenerateRequest request = OtpGenerateRequest.builder()
                                .secretKey("SEC001")
                                .purposeId("PUR001")
                                .user(user)
                                .build();

                IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                () -> otpService.generateOtp(request));
                assertEquals("Project and purpose validation failed", ex.getMessage());
                verify(redisService, never()).setWithTtl(anyString(), anyString(), anyLong());
        }

        @Test
        void testGenerateOtp_DeletesExistingOtpPriorToGenerating() {
                when(properties.getTtlSeconds()).thenReturn(300L);
                when(orgClient.validateProjectAndPurpose("SEC001", "PUR001")).thenReturn(
                                ValidationResponse.builder().valid(true).organizationId(1L).secretKey("SEC001")
                                                .message("Valid").build());
                when(redisService.exists("otp:SEC001:USR001")).thenReturn(true);

                OtpUserDto user = OtpUserDto.builder().id("USR001").build();
                OtpGenerateRequest request = OtpGenerateRequest.builder()
                                .secretKey("SEC001")
                                .purposeId("PUR001")
                                .user(user)
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
                                ValidationResponse.builder().valid(false)
                                                .message("Project not found with secret key: INVALID_SEC").build());

                OtpUserDto user = OtpUserDto.builder().id("USR001").build();
                OtpGenerateRequest request = OtpGenerateRequest.builder()
                                .secretKey("INVALID_SEC")
                                .purposeId("PUR001")
                                .user(user)
                                .build();

                IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                () -> otpService.generateOtp(request));
                assertTrue(ex.getMessage().contains("Project not found with secret key"));
                verify(redisService, never()).setWithTtl(anyString(), anyString(), anyLong());
                verify(notificationService, never()).dispatchToChannels(any(), any(), any(OtpUserDto.class), any(),
                                any(), anyLong());
        }

        @Test
        void testGenerateOtp_DifferentOrganizationThrows() {
                when(orgClient.validateProjectAndPurpose("SEC001", "PUR002")).thenReturn(
                                ValidationResponse.builder().valid(false)
                                                .message("Project and purpose do not belong to the same organization")
                                                .build());

                OtpUserDto user = OtpUserDto.builder().id("USR001").build();
                OtpGenerateRequest request = OtpGenerateRequest.builder()
                                .secretKey("SEC001")
                                .purposeId("PUR002")
                                .user(user)
                                .build();

                IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                () -> otpService.generateOtp(request));
                assertTrue(ex.getMessage().contains("do not belong to the same organization"));
                verify(redisService, never()).setWithTtl(anyString(), anyString(), anyLong());
                verify(notificationService, never()).dispatchToChannels(any(), any(), any(OtpUserDto.class), any(),
                                any(), anyLong());
        }

        @Test
        void testGenerateOtp_MissingFieldsThrows() {
                assertThrows(IllegalArgumentException.class,
                                () -> otpService.generateOtp(OtpGenerateRequest.builder().purposeId("P")
                                                .user(OtpUserDto.builder().id("U").build()).build()));
                assertThrows(IllegalArgumentException.class,
                                () -> otpService.generateOtp(
                                                OtpGenerateRequest.builder().secretKey("SEC").purposeId("P").build()));
                assertThrows(IllegalArgumentException.class,
                                () -> otpService.generateOtp(OtpGenerateRequest.builder().secretKey("SEC")
                                                .user(OtpUserDto.builder().id("U").build()).build()));
        }

        @Test
        void testVerifyOtpSuccess() {
                when(redisService.get("otp:SEC001:USR001")).thenReturn("123456");

                OtpVerifyRequest request = OtpVerifyRequest.builder()
                                .secretKey("SEC001")
                                .user(OtpUserDto.builder().id("USR001").build())
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
                                .user(OtpUserDto.builder().id("USR001").build())
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
                                .user(OtpUserDto.builder().id("USR001").build())
                                .otp("123456")
                                .build();

                IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                () -> otpService.verifyOtp(request));
                assertTrue(ex.getMessage().contains("expired or does not exist"));
                verify(redisService, never()).delete("otp:SEC001:USR001");
        }

        @Test
        void testGenerateOtp_Way1_DispatchViaInternalApi() {
                OtpService serviceWithClient = new OtpService(redisService, orgClient, properties, notificationService,
                                notificationServiceClient, notificationProperties);

                when(properties.getTtlSeconds()).thenReturn(300L);
                when(orgClient.validateProjectAndPurpose("SEC001", "PUR001")).thenReturn(
                                ValidationResponse.builder()
                                                .valid(true)
                                                .organizationId(1L)
                                                .secretKey("SEC001")
                                                .message("Valid")
                                                .channels(List.of("EMAIL", "SMS"))
                                                .build());
                when(redisService.exists("otp:SEC001:USR001")).thenReturn(false);
                when(notificationServiceClient.sendNotification(eq("SEC001"), any(OtpUserDto.class), anyString(),
                                anyString(), anyList()))
                                .thenReturn(List.of("EMAIL", "SMS"));

                OtpUserDto user = OtpUserDto.builder()
                                .id("USR001")
                                .email("user@example.com")
                                .phone("9876543210")
                                .countryCode("+91")
                                .whatsapp("+919876543210")
                                .build();

                OtpGenerateRequest request = OtpGenerateRequest.builder()
                                .secretKey("SEC001")
                                .purposeId("PUR001")
                                .user(user)
                                .dispatchMode("WAY_1")
                                .build();

                OtpGenerateResponse response = serviceWithClient.generateOtp(request);

                assertNotNull(response);
                assertEquals(List.of("EMAIL", "SMS"), response.getChannels());
                OtpUserDto expectedDispatchedUser = OtpUserDto.builder()
                                .id("USR001")
                                .email("user@example.com")
                                .phone("9876543210")
                                .countryCode("+91")
                                .build();
                verify(notificationServiceClient).sendNotification(eq("SEC001"), eq(expectedDispatchedUser), eq("OTP Verification"),
                                anyString(), eq(List.of("EMAIL", "SMS")));
                verify(notificationService, never()).dispatchToChannelsWithContent(anyList(), any(OtpUserDto.class),
                                anyString(), anyString());
        }

        @Test
        void testGenerateOtp_Way2_DispatchViaKafkaProducer() {
                OtpService serviceWithClient = new OtpService(redisService, orgClient, properties, notificationService,
                                notificationServiceClient, notificationProperties);

                when(properties.getTtlSeconds()).thenReturn(300L);
                when(orgClient.validateProjectAndPurpose("SEC001", "PUR001")).thenReturn(
                                ValidationResponse.builder()
                                                .valid(true)
                                                .organizationId(1L)
                                                .secretKey("SEC001")
                                                .message("Valid")
                                                .channels(List.of("EMAIL", "WHATSAPP"))
                                                .build());
                when(redisService.exists("otp:SEC001:USR001")).thenReturn(false);
                when(notificationService.dispatchToChannelsWithContent(eq(List.of("EMAIL", "WHATSAPP")),
                                any(OtpUserDto.class), anyString(), anyString()))
                                .thenReturn(List.of("EMAIL", "WHATSAPP"));

                OtpUserDto user = OtpUserDto.builder()
                                .id("USR001")
                                .email("user@example.com")
                                .phone("9876543210")
                                .countryCode("+91")
                                .whatsapp("+919876543210")
                                .build();

                OtpGenerateRequest request = OtpGenerateRequest.builder()
                                .secretKey("SEC001")
                                .purposeId("PUR001")
                                .user(user)
                                .dispatchMode("WAY_2")
                                .build();

                OtpGenerateResponse response = serviceWithClient.generateOtp(request);

                assertNotNull(response);
                assertEquals(List.of("EMAIL", "WHATSAPP"), response.getChannels());
                OtpUserDto expectedDispatchedUser = OtpUserDto.builder()
                                .id("USR001")
                                .email("user@example.com")
                                .countryCode("+91")
                                .whatsapp("+919876543210")
                                .build();
                verify(notificationService).dispatchToChannelsWithContent(eq(List.of("EMAIL", "WHATSAPP")), eq(expectedDispatchedUser),
                                eq("OTP Verification"), anyString());
                verify(notificationServiceClient, never()).sendNotification(anyString(), any(OtpUserDto.class),
                                anyString(), anyString(), anyList());
        }

        @Test
        void testGenerateOtp_Both_DispatchViaBothWays() {
                OtpService serviceWithClient = new OtpService(redisService, orgClient, properties, notificationService,
                                notificationServiceClient, notificationProperties);

                when(properties.getTtlSeconds()).thenReturn(300L);
                when(orgClient.validateProjectAndPurpose("SEC001", "PUR001")).thenReturn(
                                ValidationResponse.builder()
                                                .valid(true)
                                                .organizationId(1L)
                                                .secretKey("SEC001")
                                                .message("Valid")
                                                .channels(List.of("EMAIL"))
                                                .build());
                when(redisService.exists("otp:SEC001:USR001")).thenReturn(false);
                when(notificationServiceClient.sendNotification(eq("SEC001"), any(OtpUserDto.class), anyString(),
                                anyString(), anyList()))
                                .thenReturn(List.of("EMAIL"));
                when(notificationService.dispatchToChannelsWithContent(eq(List.of("EMAIL")), any(OtpUserDto.class),
                                anyString(), anyString()))
                                .thenReturn(List.of("EMAIL"));

                OtpUserDto user = OtpUserDto.builder()
                                .id("USR001")
                                .email("user@example.com")
                                .build();

                OtpGenerateRequest request = OtpGenerateRequest.builder()
                                .secretKey("SEC001")
                                .purposeId("PUR001")
                                .user(user)
                                .dispatchMode("BOTH")
                                .build();

                OtpGenerateResponse response = serviceWithClient.generateOtp(request);

                assertNotNull(response);
                assertEquals(List.of("EMAIL"), response.getChannels());
                verify(notificationServiceClient).sendNotification(eq("SEC001"), eq(user), eq("OTP Verification"),
                                anyString(), eq(List.of("EMAIL")));
                verify(notificationService).dispatchToChannelsWithContent(eq(List.of("EMAIL")), eq(user),
                                eq("OTP Verification"), anyString());
        }

        @Test
        void testFilterUserByChannels_OnlyEmail() {
                OtpUserDto user = OtpUserDto.builder()
                                .id("USR001")
                                .email("test@example.com")
                                .phone("9876543210")
                                .countryCode("+91")
                                .whatsapp("+919876543210")
                                .build();

                OtpUserDto filtered = otpService.filterUserByChannels(user, List.of("EMAIL"));

                assertNotNull(filtered);
                assertEquals("USR001", filtered.getId());
                assertEquals("test@example.com", filtered.getEmail());
                assertNull(filtered.getPhone());
                assertNull(filtered.getCountryCode());
                assertNull(filtered.getWhatsapp());
        }

        @Test
        void testFilterUserByChannels_OnlySmsOrMobile() {
                OtpUserDto user = OtpUserDto.builder()
                                .id("USR001")
                                .email("test@example.com")
                                .phone("9876543210")
                                .countryCode("+91")
                                .whatsapp("+919876543210")
                                .build();

                OtpUserDto filtered = otpService.filterUserByChannels(user, List.of("SMS"));

                assertNotNull(filtered);
                assertEquals("USR001", filtered.getId());
                assertNull(filtered.getEmail());
                assertEquals("9876543210", filtered.getPhone());
                assertEquals("+91", filtered.getCountryCode());
                assertNull(filtered.getWhatsapp());
        }

        @Test
        void testFilterUserByChannels_OnlyWhatsapp() {
                OtpUserDto user = OtpUserDto.builder()
                                .id("USR001")
                                .email("test@example.com")
                                .phone("9876543210")
                                .countryCode("+91")
                                .whatsapp("+919876543210")
                                .build();

                OtpUserDto filtered = otpService.filterUserByChannels(user, List.of("WHATSAPP"));

                assertNotNull(filtered);
                assertEquals("USR001", filtered.getId());
                assertNull(filtered.getEmail());
                assertNull(filtered.getPhone());
                assertEquals("+91", filtered.getCountryCode());
                assertEquals("+919876543210", filtered.getWhatsapp());
        }

        @Test
        void testFilterUserByChannels_EmptyChannelsOrNull() {
                OtpUserDto user = OtpUserDto.builder()
                                .id("USR001")
                                .email("test@example.com")
                                .phone("9876543210")
                                .countryCode("+91")
                                .whatsapp("+919876543210")
                                .build();

                OtpUserDto filteredEmpty = otpService.filterUserByChannels(user, List.of());
                assertNotNull(filteredEmpty);
                assertEquals("USR001", filteredEmpty.getId());
                assertNull(filteredEmpty.getEmail());
                assertNull(filteredEmpty.getPhone());

                assertNull(otpService.filterUserByChannels(null, List.of("EMAIL")));
        }
}

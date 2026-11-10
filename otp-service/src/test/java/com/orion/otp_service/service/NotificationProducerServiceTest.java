package com.orion.otp_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.orion.otp_service.config.NotificationProperties;
import com.orion.otp_service.dto.OtpUserDto;

class NotificationProducerServiceTest {

    private NotificationProperties properties;
    private NotificationProducerService service;

    @BeforeEach
    void setUp() {
        properties = new NotificationProperties();
        properties.setUrl(System.getenv().getOrDefault("NOTIFICATION_SERVICE_URL", "https://localhost:21340"));
        properties.setUsername(System.getenv().getOrDefault("NOTIFICATION_SERVICE_USER", "test-user"));
        properties.setPassword(System.getenv().getOrDefault("NOTIFICATION_SERVICE_PASSWORD", ""));
        properties.setTopicEmail("TOPIC_EMAIL");
        properties.setTopicSms("TOPIC_SMS");
        properties.setTopicWhatsapp("TOPIC_WHATSAPP");

        service = new NotificationProducerService(RestClient.builder(), properties);
    }

    @Test
    void testResolveTopicForChannel() {
        assertEquals("TOPIC_SMS", service.resolveTopicForChannel("SMS"));
        assertEquals("TOPIC_SMS", service.resolveTopicForChannel("sms"));
        assertEquals("TOPIC_EMAIL", service.resolveTopicForChannel("EMAIL"));
        assertEquals("TOPIC_EMAIL", service.resolveTopicForChannel("email"));
        assertEquals("TOPIC_EMAIL", service.resolveTopicForChannel("MAIL"));
        assertEquals("TOPIC_WHATSAPP", service.resolveTopicForChannel("WHATSAPP"));
        assertEquals("TOPIC_WHATSAPP", service.resolveTopicForChannel("whatsapp"));
        assertNull(service.resolveTopicForChannel("UNKNOWN"));
        assertNull(service.resolveTopicForChannel(null));
    }

    @Test
    void testResolveReceiverForChannel() {
        OtpUserDto user = OtpUserDto.builder()
                .id("USR001")
                .email("user@example.com")
                .phone("9876543210")
                .countryCode("+91")
                .whatsapp("+918888888888")
                .build();

        assertEquals("user@example.com", service.resolveReceiverForChannel("EMAIL", user));
        assertEquals("+919876543210", service.resolveReceiverForChannel("SMS", user));
        assertEquals("+918888888888", service.resolveReceiverForChannel("WHATSAPP", user));

        // When whatsapp field is empty, fallback to phone
        OtpUserDto userNoWa = OtpUserDto.builder()
                .id("USR002")
                .email("user2@example.com")
                .phone("9123456780")
                .countryCode("+1")
                .build();
        assertEquals("+19123456780", service.resolveReceiverForChannel("WHATSAPP", userNoWa));

        // When user fields are missing, fallback to userId
        OtpUserDto userOnlyId = OtpUserDto.builder().id("USR003").build();
        assertEquals("USR003", service.resolveReceiverForChannel("EMAIL", userOnlyId));
        assertEquals("USR003", service.resolveReceiverForChannel("SMS", userOnlyId));
        assertEquals("USR003", service.resolveReceiverForChannel("WHATSAPP", userOnlyId));
    }

    @Test
    void testDispatchToChannels_EmptyOrNullReturnsEmptyList() {
        List<String> resultNull = service.dispatchToChannels(null, "sec", OtpUserDto.builder().id("usr").build(), "pur",
                "123456", 300);
        assertTrue(resultNull.isEmpty());

        List<String> resultEmpty = service.dispatchToChannels(List.of(), "sec", OtpUserDto.builder().id("usr").build(),
                "pur", "123456", 300);
        assertTrue(resultEmpty.isEmpty());
    }

    @Test
    void testDispatchToChannels_LiveAivenKafkaDispatch() {
        Assumptions.assumeTrue(
                properties.getPassword() != null && !properties.getPassword().isBlank()
                        && "true".equalsIgnoreCase(System.getenv("RUN_LIVE_KAFKA_TESTS")),
                "Live Aiven Kafka integration test skipped (enable by setting RUN_LIVE_KAFKA_TESTS=true with valid credentials)");

        // Live integration test with real Aiven Kafka REST cluster
        List<String> dispatched = service.dispatchToChannels(
                List.of("SMS", "WHATSAPP", "EMAIL"),
                "sec-test-123",
                OtpUserDto.builder().id("USR-UNITTEST").build(),
                "PUR-UNITTEST",
                "999888",
                300);

        assertEquals(3, dispatched.size());
        assertTrue(dispatched.contains("SMS"));
        assertTrue(dispatched.contains("WHATSAPP"));
        assertTrue(dispatched.contains("EMAIL"));
    }

    @Test
    void testDispatchToChannels_WithOtpUserDto() {
        Assumptions.assumeTrue(
                properties.getPassword() != null && !properties.getPassword().isBlank()
                        && "true".equalsIgnoreCase(System.getenv("RUN_LIVE_KAFKA_TESTS")),
                "Live Aiven Kafka integration test skipped (enable by setting RUN_LIVE_KAFKA_TESTS=true with valid credentials)");

        OtpUserDto user = OtpUserDto.builder()
                .id("USR-UNITTEST")
                .email("test@example.com")
                .phone("9876543210")
                .countryCode("+1")
                .whatsapp("+19876543210")
                .build();

        List<String> dispatched = service.dispatchToChannels(
                List.of("SMS", "WHATSAPP", "EMAIL"),
                "sec-test-123",
                user,
                "PUR-UNITTEST",
                "999888",
                300);

        assertEquals(3, dispatched.size());
        assertTrue(dispatched.contains("SMS"));
        assertTrue(dispatched.contains("WHATSAPP"));
        assertTrue(dispatched.contains("EMAIL"));
    }
}

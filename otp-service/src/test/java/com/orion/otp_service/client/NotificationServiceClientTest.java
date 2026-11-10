package com.orion.otp_service.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.orion.otp_service.dto.OtpUserDto;

class NotificationServiceClientTest {

    private MockRestServiceServer mockServer;
    private NotificationServiceClient client;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        RestClient.Builder builder = RestClient.builder()
                .messageConverters(
                        converters -> converters.add(0, new MappingJackson2HttpMessageConverter(objectMapper)));
        mockServer = MockRestServiceServer.bindTo(builder).build();
        client = new NotificationServiceClient(builder);
    }

    @Test
    void testSendNotification_Success_Way1InternalApi() {
        String jsonResponse = """
                {
                    "success": true,
                    "message": "Notification dispatched to channels: [EMAIL, SMS]",
                    "data": {
                        "secretKey": "SEC001",
                        "user": {
                            "id": "USR001",
                            "email": "user@example.com",
                            "phone": "9876543210",
                            "countryCode": "+91",
                            "whatsapp": "+919876543210"
                        },
                        "title": "Your OTP",
                        "description": "Your OTP is 123456",
                        "dispatchedChannels": ["EMAIL", "SMS"]
                    }
                }
                """;

        mockServer.expect(requestTo("http://notification-service/api/notifications/internal/v1/send"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.secretKey").value("SEC001"))
                .andExpect(jsonPath("$.user.id").value("USR001"))
                .andExpect(jsonPath("$.user.email").value("user@example.com"))
                .andExpect(jsonPath("$.user.phone").value("9876543210"))
                .andExpect(jsonPath("$.user.countryCode").value("+91"))
                .andExpect(jsonPath("$.user.whatsapp").value("+919876543210"))
                .andExpect(jsonPath("$.title").value("Your OTP"))
                .andExpect(jsonPath("$.description").value("Your OTP is 123456"))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        OtpUserDto user = OtpUserDto.builder()
                .id("USR001")
                .email("user@example.com")
                .phone("9876543210")
                .countryCode("+91")
                .whatsapp("+919876543210")
                .build();

        List<String> dispatched = client.sendNotification("SEC001", user, "Your OTP", "Your OTP is 123456");

        mockServer.verify();
        assertNotNull(dispatched);
        assertEquals(2, dispatched.size());
        assertEquals(List.of("EMAIL", "SMS"), dispatched);
    }

    @Test
    void testSendNotification_ServerErrorHandledGracefully() {
        mockServer.expect(requestTo("http://notification-service/api/notifications/internal/v1/send"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        OtpUserDto user = OtpUserDto.builder().id("USR001").build();
        List<String> dispatched = client.sendNotification("SEC001", user, "Title", "Body");

        mockServer.verify();
        assertNotNull(dispatched);
        assertTrue(dispatched.isEmpty());
    }
}

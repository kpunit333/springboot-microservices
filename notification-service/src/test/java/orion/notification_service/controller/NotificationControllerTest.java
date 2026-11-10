package orion.notification_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import orion.notification_service.constant.Channel;
import orion.notification_service.dto.NotificationSendResponse;
import orion.notification_service.exception.GlobalExceptionHandler;
import orion.notification_service.exception.InvalidSecretKeyException;
import orion.notification_service.exception.ProjectInactiveException;
import orion.notification_service.service.NotificationService;

class NotificationControllerTest {

    private MockMvc mockMvc;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = mock(NotificationService.class);
        NotificationController controller = new NotificationController(notificationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testSendInternal_success() throws Exception {
        NotificationSendResponse mockResponse = NotificationSendResponse.builder()
                .status("SENT")
                .dispatchedChannels(List.of(Channel.EMAIL, Channel.SMS))
                .title("Welcome")
                .count(2)
                .build();

        when(notificationService.sendInternal(any())).thenReturn(mockResponse);

        String jsonPayload = """
                {
                    "secretKey": "some-key",
                    "user": {
                        "id": "usr_100",
                        "email": "user@test.com",
                        "phone": "9876543210",
                        "countryCode": "+91",
                        "whatsapp": "9876543210"
                    },
                    "title": "Welcome",
                    "description": "Your account is created"
                }
                """;

        mockMvc.perform(post("/api/notifications/internal/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Notification dispatched successfully (internal)"))
                .andExpect(jsonPath("$.data.status").value("SENT"))
                .andExpect(jsonPath("$.data.count").value(2))
                .andExpect(jsonPath("$.data.dispatchedChannels[0]").value("EMAIL"))
                .andExpect(jsonPath("$.data.dispatchedChannels[1]").value("SMS"));

        // Also test /api/notifications/internal/v1/send
        mockMvc.perform(post("/api/notifications/internal/v1/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testSendV1_success() throws Exception {
        NotificationSendResponse mockResponse = NotificationSendResponse.builder()
                .status("SENT")
                .dispatchedChannels(List.of(Channel.EMAIL, Channel.SMS, Channel.WHATSAPP))
                .title("Order Alert")
                .count(3)
                .build();

        when(notificationService.sendV1(any())).thenReturn(mockResponse);

        String jsonPayload = """
                {
                    "secretKey": "123e4567-e89b-12d3-a456-426614174000",
                    "user": {
                        "id": "usr_200",
                        "email": "client@test.com",
                        "phone": "9876543210",
                        "countryCode": "+91",
                        "whatsapp": "9876543210"
                    },
                    "title": "Order Alert",
                    "description": "Your order has been shipped"
                }
                """;

        mockMvc.perform(post("/api/notifications/v1/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Notification dispatched successfully"))
                .andExpect(jsonPath("$.data.status").value("SENT"))
                .andExpect(jsonPath("$.data.count").value(3));
    }

    @Test
    void testSendV1_invalidSecretKey_returns401() throws Exception {
        when(notificationService.sendV1(any()))
                .thenThrow(new InvalidSecretKeyException("Invalid secret key"));

        String jsonPayload = """
                {
                    "secretKey": "bad-key",
                    "user": {
                        "id": "usr_300"
                    },
                    "title": "Hello",
                    "description": "World"
                }
                """;

        mockMvc.perform(post("/api/notifications/v1/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid secret key"));
    }

    @Test
    void testSendV1_inactiveProject_returns403() throws Exception {
        when(notificationService.sendV1(any()))
                .thenThrow(new ProjectInactiveException("Project is not active"));

        String jsonPayload = """
                {
                    "secretKey": "123e4567-e89b-12d3-a456-426614174000",
                    "user": {
                        "id": "usr_400"
                    },
                    "title": "Hello",
                    "description": "World"
                }
                """;

        mockMvc.perform(post("/api/notifications/v1/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Project is not active"));
    }
}

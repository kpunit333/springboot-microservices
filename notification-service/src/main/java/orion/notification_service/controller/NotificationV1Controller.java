package orion.notification_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import orion.notification_service.dto.ApiResponse;
import orion.notification_service.dto.NotificationSendRequest;
import orion.notification_service.dto.NotificationSendResponse;
import orion.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/notifications/v1")
@RequiredArgsConstructor
public class NotificationV1Controller {

    private final NotificationService notificationService;

    /**
     * V1 public/client endpoint: verifies the secret key against the project table
     * and sends notification to all the subscribed channels configured for the
     * project.
     * Accessible via /api/notifications/v1/send.
     */
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<NotificationSendResponse>> sendV1(
            @RequestBody NotificationSendRequest request) {
        log.info("Received v1 notification send request for project secretKey: {}", request.getSecretKey());

        NotificationSendResponse response = notificationService.sendV1(request);
        return ResponseEntity.ok(ApiResponse.ok("Notification dispatched successfully", response));
    }
}

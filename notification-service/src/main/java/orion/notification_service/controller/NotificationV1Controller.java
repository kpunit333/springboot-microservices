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

/**
 * Controller for V1 public/client notification endpoints.
 * Verifies the secret key against the project table and sends notifications
 * to all subscribed channels configured for the project.
 *
 * Endpoints:
 * POST /api/notifications/v1/send
 * POST /api/notifications/send
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications/v1")
public class NotificationV1Controller {

    private final NotificationService notificationService;

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<NotificationSendResponse>> sendV1(
            @RequestBody NotificationSendRequest request) {
        log.info("Received v1 notification send request for project secretKey: {}",
                request != null ? request.getSecretKey() : null);

        NotificationSendResponse response = notificationService.sendV1(request);
        return ResponseEntity.ok(ApiResponse.ok("Notification dispatched successfully", response));
    }
}

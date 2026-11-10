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
 * Controller for internal service-to-service notification endpoints.
 * Does not verify the project secret key and dispatches notifications directly.
 *
 * Endpoints:
 * POST /api/notifications/internal/v1/send
 * POST /api/notifications/internal/send
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping({ "/api/notifications/internal/v1", "/api/notifications/internal" })
public class NotificationInternalV1Controller {

    private final NotificationService notificationService;

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<NotificationSendResponse>> sendInternal(
            @RequestBody NotificationSendRequest request) {
        String userId = (request != null && request.getUser() != null) ? request.getUser().getId() : "null";
        log.info("Received internal notification send request for user: {}", userId);

        NotificationSendResponse response = notificationService.sendInternal(request);
        return ResponseEntity.ok(ApiResponse.ok("Notification dispatched successfully (internal)", response));
    }
}

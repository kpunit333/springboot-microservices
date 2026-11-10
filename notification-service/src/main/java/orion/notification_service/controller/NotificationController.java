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
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Internal endpoint: does not verify the secret key.
     * Dispatches notification to all subscribed / eligible channels.
     * Accessible via both /api/notifications/internal/v1/send and
     * /api/notifications/internal/send.
     */
    @PostMapping({ "/api/notifications/internal/v1/send", "/api/notifications/internal/send" })
    public ResponseEntity<ApiResponse<NotificationSendResponse>> sendInternal(
            @RequestBody NotificationSendRequest request) {
        log.info("Received internal notification send request for user: {}",
                request.getUser() != null ? request.getUser().getId() : "null");

        NotificationSendResponse response = notificationService.sendInternal(request);
        return ResponseEntity.ok(ApiResponse.ok("Notification dispatched successfully (internal)", response));
    }

    /**
     * Public V1 endpoint: verifies project secret key against PostgreSQL before dispatch.
     * Accessible via both /api/notifications/v1/send and /api/notifications/send.
     */
    @PostMapping({ "/api/notifications/v1/send", "/api/notifications/send" })
    public ResponseEntity<ApiResponse<NotificationSendResponse>> sendV1(
            @RequestBody NotificationSendRequest request) {
        log.info("Received v1 notification send request for user: {}",
                request.getUser() != null ? request.getUser().getId() : "null");

        NotificationSendResponse response = notificationService.sendV1(request);
        return ResponseEntity.ok(ApiResponse.ok("Notification dispatched successfully", response));
    }
}

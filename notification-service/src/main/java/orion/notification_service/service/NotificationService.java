package orion.notification_service.service;

import orion.notification_service.dto.NotificationSendRequest;
import orion.notification_service.dto.NotificationSendResponse;

public interface NotificationService {

    /**
     * Sends notification to subscribed channels without verifying the secret key.
     */
    NotificationSendResponse sendInternal(NotificationSendRequest request);

    /**
     * Verifies the secret key from the project table and sends notification to all subscribed channels.
     */
    NotificationSendResponse sendV1(NotificationSendRequest request);
}


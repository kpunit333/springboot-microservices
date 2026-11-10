package orion.notification_service.consumer;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import orion.notification_service.dto.ProducerPayload;

/**
 * Consumer for TOPIC_SMS messages.
 */
@Slf4j
@Component
public class SmsConsumer {

    public void consume(ProducerPayload payload) {
        if (payload == null) {
            log.warn("[SMS Consumer] Received null payload, skipping.");
            return;
        }

        String receiver = payload.getReceiver();
        String title = payload.getTitle();
        String description = payload.getDescription();

        log.info("[SMS Consumer] Received SMS message for receiver='{}', title='{}', body='{}'",
                receiver, title, description);
    }
}


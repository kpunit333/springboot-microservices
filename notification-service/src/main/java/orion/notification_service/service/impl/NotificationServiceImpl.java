package orion.notification_service.service.impl;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import orion.notification_service.constant.Channel;
import orion.notification_service.dto.NotificationSendRequest;
import orion.notification_service.dto.NotificationSendResponse;
import orion.notification_service.dto.NotificationUserDto;
import orion.notification_service.dto.ProducerPayload;
import orion.notification_service.exception.InvalidSecretKeyException;
import orion.notification_service.exception.ProjectExpiredException;
import orion.notification_service.exception.ProjectInactiveException;
import orion.notification_service.model.Project;
import orion.notification_service.producer.NotificationProducer;
import orion.notification_service.repository.ProjectRepository;
import orion.notification_service.service.NotificationService;
import orion.notification_service.util.PhoneUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final ProjectRepository projectRepository;
    private final NotificationProducer notificationProducer;

    @Override
    public NotificationSendResponse sendInternal(NotificationSendRequest request) {
        log.info("Processing internal notification send request (secretKey check skipped)");

        List<String> channels = resolveChannelsForInternal(request);
        return dispatchNotifications(channels, request);
    }

    @Override
    public NotificationSendResponse sendV1(NotificationSendRequest request) {
        log.info("Processing v1 notification send request (verifying secretKey)");

        Project project = verifyAndGetProject(request.getSecretKey());
        List<String> channels = project.getChannels() != null ? project.getChannels() : List.of();
        log.info("Verified project '{}' (ID: {}). Subscribed channels: {}", project.getName(), project.getId(), channels);

        return dispatchNotifications(channels, request);
    }

    private Project verifyAndGetProject(String secretKeyStr) {
        if (secretKeyStr == null || secretKeyStr.trim().isEmpty()) {
            throw new InvalidSecretKeyException("secretKey is required for v1 notifications");
        }

        UUID secretKey;
        try {
            secretKey = UUID.fromString(secretKeyStr.trim());
        } catch (IllegalArgumentException e) {
            throw new InvalidSecretKeyException("Invalid secretKey format: " + secretKeyStr);
        }

        Project project = projectRepository.findBySecretKey(secretKey)
                .orElseThrow(() -> new InvalidSecretKeyException("Project not found with secretKey: " + secretKey));

        if (project.getStatus() != null && !project.getStatus().equalsIgnoreCase("ACTIVE")) {
            throw new ProjectInactiveException("Project is not active. Current status: " + project.getStatus());
        }

        if (project.getExpireAt() != null && project.getExpireAt().isBefore(OffsetDateTime.now())) {
            throw new ProjectExpiredException("Project expired at: " + project.getExpireAt());
        }

        return project;
    }

    private List<String> resolveChannelsForInternal(NotificationSendRequest request) {
        if (request.getChannels() != null) {
            List<String> explicit = request.getChannels().stream()
                    .filter(c -> c != null && !c.trim().isEmpty())
                    .map(String::trim)
                    .toList();
            log.info("Using channels explicitly specified in internal request: {}", explicit);
            return explicit;
        }
        if (request.getSecretKey() != null && !request.getSecretKey().trim().isEmpty()) {
            try {
                UUID secretKey = UUID.fromString(request.getSecretKey().trim());
                Optional<Project> projectOpt = projectRepository.findBySecretKey(secretKey);
                if (projectOpt.isPresent()) {
                    List<String> projectChannels = projectOpt.get().getChannels();
                    if (projectChannels != null && !projectChannels.isEmpty()) {
                        return projectChannels;
                    }
                }
            } catch (IllegalArgumentException ignored) {
                log.debug("SecretKey in internal request is not a UUID, falling back to all available channels");
            }
        }
        // Fallback to all standard channels when secretKey is unverified or project is not present
        return List.of(Channel.EMAIL, Channel.SMS, Channel.WHATSAPP);
    }

    private NotificationSendResponse dispatchNotifications(List<String> subscribedChannels, NotificationSendRequest request) {
        NotificationUserDto user = request.getUser();
        String title = request.getTitle() != null ? request.getTitle() : "";
        String description = request.getDescription() != null ? request.getDescription() : "";
        List<String> dispatched = new ArrayList<>();

        if (subscribedChannels == null || subscribedChannels.isEmpty()) {
            log.warn("No subscribed channels available for notification dispatch");
            return NotificationSendResponse.builder()
                    .status("NO_CHANNELS")
                    .dispatchedChannels(dispatched)
                    .title(title)
                    .count(0)
                    .build();
        }

        boolean emailSubscribed = subscribedChannels.stream()
                .anyMatch(c -> c != null && (c.equalsIgnoreCase(Channel.EMAIL) || c.equalsIgnoreCase("MAIL")));

        boolean smsSubscribed = subscribedChannels.stream()
                .anyMatch(c -> c != null && c.equalsIgnoreCase(Channel.SMS));

        boolean whatsAppSubscribed = subscribedChannels.stream()
                .anyMatch(c -> c != null && (c.equalsIgnoreCase(Channel.WHATSAPP) || c.equalsIgnoreCase("WA")));

        // 1. Email Producer Dispatch
        if (emailSubscribed) {
            String email = (user != null && user.getEmail() != null) ? user.getEmail().trim() : null;
            if (email != null && !email.isEmpty()) {
                ProducerPayload emailPayload = ProducerPayload.builder()
                        .receiver(email)
                        .title(title)
                        .description(description)
                        .timestamp(Instant.now().toString())
                        .build();
                notificationProducer.sendEmail(emailPayload);
                dispatched.add(Channel.EMAIL);
            } else {
                log.warn("Email channel subscribed but user email is empty or null");
            }
        }

        // 2. SMS Producer Dispatch
        if (smsSubscribed) {
            String phone = (user != null && user.getPhone() != null) ? user.getPhone().trim() : null;
            if (phone != null && !phone.isEmpty()) {
                String receiver = PhoneUtils.formatReceiverPhone(user.getCountryCode(), phone);
                ProducerPayload smsPayload = ProducerPayload.builder()
                        .receiver(receiver)
                        .title(title)
                        .description(description)
                        .timestamp(Instant.now().toString())
                        .build();
                notificationProducer.sendSms(smsPayload);
                dispatched.add(Channel.SMS);
            } else {
                log.warn("SMS channel subscribed but user phone is empty or null");
            }
        }

        // 3. WhatsApp Producer Dispatch
        if (whatsAppSubscribed) {
            String waTarget = null;
            if (user != null) {
                if (user.getWhatsapp() != null && !user.getWhatsapp().trim().isEmpty()) {
                    waTarget = user.getWhatsapp().trim();
                } else if (user.getPhone() != null && !user.getPhone().trim().isEmpty()) {
                    waTarget = user.getPhone().trim();
                }
            }

            if (waTarget != null && !waTarget.isEmpty()) {
                String receiver = PhoneUtils.formatReceiverPhone(user.getCountryCode(), waTarget);
                ProducerPayload waPayload = ProducerPayload.builder()
                        .receiver(receiver)
                        .title(title)
                        .description(description)
                        .timestamp(Instant.now().toString())
                        .build();
                notificationProducer.sendWhatsApp(waPayload);
                dispatched.add(Channel.WHATSAPP);
            } else {
                log.warn("WhatsApp channel subscribed but user whatsapp/phone is empty or null");
            }
        }

        String status = dispatched.isEmpty() ? "FAILED" : "SENT";
        return NotificationSendResponse.builder()
                .status(status)
                .dispatchedChannels(dispatched)
                .title(title)
                .count(dispatched.size())
                .build();
    }
}

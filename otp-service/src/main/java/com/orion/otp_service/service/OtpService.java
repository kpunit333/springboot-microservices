package com.orion.otp_service.service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.orion.otp_service.client.NotificationServiceClient;
import com.orion.otp_service.client.OrganizationServiceClient;
import com.orion.otp_service.config.NotificationProperties;
import com.orion.otp_service.config.OtpProperties;
import com.orion.otp_service.dto.OtpGenerateRequest;
import com.orion.otp_service.dto.OtpGenerateResponse;
import com.orion.otp_service.dto.OtpUserDto;
import com.orion.otp_service.dto.OtpVerifyRequest;
import com.orion.otp_service.dto.ValidationResponse;
import com.orion.otp_service.template.OtpNotificationTemplate;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OtpService {

    private final UpstashRedisService redisService;
    private final OrganizationServiceClient orgClient;
    private final OtpProperties properties;
    private final NotificationProducerService notificationProducerService;
    private final NotificationServiceClient notificationServiceClient;
    private final NotificationProperties notificationProperties;
    private final OtpNotificationTemplate otpNotificationTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public OtpService(
            UpstashRedisService redisService,
            OrganizationServiceClient orgClient,
            OtpProperties properties,
            NotificationProducerService notificationProducerService,
            @Autowired(required = false) NotificationServiceClient notificationServiceClient,
            @Autowired(required = false) NotificationProperties notificationProperties,
            @Autowired(required = false) OtpNotificationTemplate otpNotificationTemplate) {
        this.redisService = redisService;
        this.orgClient = orgClient;
        this.properties = properties;
        this.notificationProducerService = notificationProducerService;
        this.notificationServiceClient = notificationServiceClient;
        this.notificationProperties = notificationProperties != null ? notificationProperties
                : new NotificationProperties();
        this.otpNotificationTemplate = otpNotificationTemplate != null ? otpNotificationTemplate
                : new OtpNotificationTemplate();
    }

    public OtpService(
            UpstashRedisService redisService,
            OrganizationServiceClient orgClient,
            OtpProperties properties,
            NotificationProducerService notificationProducerService,
            NotificationServiceClient notificationServiceClient,
            NotificationProperties notificationProperties) {
        this(redisService, orgClient, properties, notificationProducerService, notificationServiceClient,
                notificationProperties, new OtpNotificationTemplate());
    }

    public OtpService(
            UpstashRedisService redisService,
            OrganizationServiceClient orgClient,
            OtpProperties properties,
            NotificationProducerService notificationProducerService) {
        this(redisService, orgClient, properties, notificationProducerService, null, new NotificationProperties(),
                new OtpNotificationTemplate());
    }

    /**
     * Generates a 6-digit OTP for the restricted /api/otp/v1/generate endpoint.
     * Validates secretKey and purposeId against organization-service.
     * Fetches channels configured in the project table and dispatches payload.
     * Deletes any existing OTP for the same (secretKey, user.id) pair.
     * Stores the new OTP in Redis with a 5-minute TTL.
     */
    public OtpGenerateResponse generateOtp(OtpGenerateRequest request) {
        return generateOtpInternal(request, false);
    }

    /**
     * Generates a 6-digit OTP for the test endpoints (/test/generate,
     * /api/otp/v1/test/generate).
     */
    public OtpGenerateResponse generateTestOtp(OtpGenerateRequest request) {
        return generateOtpInternal(request, true);
    }

    private OtpGenerateResponse generateOtpInternal(OtpGenerateRequest request, boolean isTest) {
        if (request == null) {
            throw new IllegalArgumentException("Request body cannot be null");
        }

        String secretKey = sanitize(request.getSecretKey());
        String purposeId = sanitize(request.getPurposeId());
        OtpUserDto user = request.getUser();
        String userId = user != null ? sanitize(user.getId()) : null;

        if (secretKey == null || secretKey.isEmpty()) {
            throw new IllegalArgumentException("Secret key is required");
        }
        if (purposeId == null || purposeId.isEmpty()) {
            throw new IllegalArgumentException("Purpose ID is required");
        }
        if (user == null || userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("User and User ID are required");
        }

        String effectiveSecretKey = secretKey;
        String projectName = null;
        String purposeName = null;
        List<String> targetChannels;
        boolean filterUser;

        if (!isTest) {
            // 1. Production /generate: check project, purpose, and channels from DB
            ValidationResponse validation;
            try {
                validation = orgClient.validateProjectAndPurpose(secretKey, purposeId);
            } catch (Exception e) {
                log.error(
                        "Organization service is unavailable or returned an error for secretKey='{}', purposeId='{}': {}",
                        secretKey, purposeId, e.getMessage());
                throw new IllegalArgumentException(
                        "Organization service is unavailable. Please try again later.");
            }
            if (validation == null || !validation.isValid()) {
                String msg = (validation != null && validation.getMessage() != null)
                        ? validation.getMessage()
                        : "Project and purpose validation failed";
                log.warn("Validation failed for secretKey='{}', purposeId='{}': {}", secretKey, purposeId, msg);
                throw new IllegalArgumentException(msg);
            }

            targetChannels = extractChannels(validation.getChannels());
            if (validation.getSecretKey() != null) {
                effectiveSecretKey = validation.getSecretKey();
            }
            if (validation.getProjectName() != null && !validation.getProjectName().isBlank()) {
                projectName = validation.getProjectName();
            }
            if (validation.getPurposeName() != null && !validation.getPurposeName().isBlank()) {
                purposeName = validation.getPurposeName();
            }
            filterUser = true;
            log.debug(
                    "Delivery channels fetched from project table: {}, projectName='{}', purposeName='{}' (isTest=false)",
                    targetChannels, projectName, purposeName);
        } else {
            // 1. Test /test/generate: use only valid, non-blank channels specified in the
            // request;
            // if none or blank provided, default to empty list (no dispatch)
            targetChannels = extractChannels(request.getChannels());
            filterUser = false;
            log.info(
                    "Test mode: using channels from request (empty=no dispatch): {}, projectName='{}', purposeName='{}' (isTest=true)",
                    targetChannels, projectName, purposeName);
        }

        // 2. Check if OTP from same secretKey and userId is already present; if so,
        // delete it
        String redisKey = buildKey(effectiveSecretKey, userId);
        if (redisService.exists(redisKey)) {
            log.info("Existing OTP found for key '{}'. Deleting prior to new generation.", redisKey);
            redisService.delete(redisKey);
        }

        // 3. Generate 6-digit OTP
        int number = secureRandom.nextInt(1_000_000);
        String otp = String.format("%06d", number);

        // 4. Store in Redis with TTL 5 minutes (default 300 seconds)
        long ttlSeconds = properties.getTtlSeconds();
        redisService.setWithTtl(redisKey, otp, ttlSeconds);
        log.info("Generated and saved 6-digit OTP for key '{}' with TTL {}s (isTest={})", redisKey, ttlSeconds, isTest);

        // 5. Dispatch notifications using Way 1, Way 2, or Both only if target channels
        // are present
        List<String> dispatched = List.of();
        if (!targetChannels.isEmpty()) {
            dispatched = dispatchNotification(
                    targetChannels,
                    effectiveSecretKey,
                    projectName,
                    purposeName,
                    user,
                    purposeId,
                    otp,
                    ttlSeconds,
                    request.getDispatchMode(),
                    filterUser);
        } else {
            log.info("No delivery channels configured/specified (isTest={}). Skipping notification dispatch.", isTest);
        }

        return OtpGenerateResponse.builder()
                .secretKey(effectiveSecretKey)
                .purposeId(purposeId)
                .user(user)
                .otp(otp)
                .expiresInSeconds(ttlSeconds)
                .channels(targetChannels.isEmpty() ? List.of()
                        : (dispatched != null && !dispatched.isEmpty() ? dispatched : targetChannels))
                .build();
    }

    /**
     * Way 1: Dispatches notification via notification-service internal endpoint
     * (/api/notifications/internal/v1/send).
     */
    public List<String> dispatchViaInternalApi(String secretKey, OtpUserDto user, String title, String message) {
        return dispatchViaInternalApi(secretKey, user, title, message, null);
    }

    public List<String> dispatchViaInternalApi(String secretKey, OtpUserDto user, String title, String message,
            List<String> channels) {
        log.info("Dispatching notification via Way 1: /api/notifications/internal/v1/send to channels: {}", channels);
        if (notificationServiceClient != null) {
            return notificationServiceClient.sendNotification(secretKey, user, title, message, channels);
        }
        return List.of();
    }

    /**
     * Way 2: Produces notifications directly to Kafka topics for all subscribed
     * channels
     * just like the notification service.
     */
    public List<String> dispatchViaKafkaProducer(List<String> targetChannels, OtpUserDto user, String title,
            String message) {
        log.info("Dispatching notification via Way 2: Kafka topics producer");
        if (notificationProducerService != null) {
            return notificationProducerService.dispatchToChannelsWithContent(targetChannels, user, title, message);
        }
        return List.of();
    }

    /**
     * Filters user contact fields based on the project's subscribed channels before
     * dispatching:
     * - If Email is present, only user.email is retained.
     * - If SMS / Mobile / Phone is present, only user.phone/mobile (and
     * countryCode) is retained.
     * - If WhatsApp is present, only user.whatsapp (and countryCode) is retained.
     * Fields for unsubscribed channels are set to null so they are omitted from
     * dispatch payloads.
     */
    public OtpUserDto filterUserByChannels(OtpUserDto user, List<String> channels) {
        if (user == null) {
            return null;
        }

        if (channels == null || channels.isEmpty()) {
            return OtpUserDto.builder()
                    .id(user.getId())
                    .build();
        }

        boolean hasEmail = channels.stream()
                .anyMatch(c -> c != null && (c.equalsIgnoreCase("EMAIL") || c.equalsIgnoreCase("MAIL")));

        boolean hasMobile = channels.stream()
                .anyMatch(c -> c != null && (c.equalsIgnoreCase("SMS")
                        || c.equalsIgnoreCase("MOBILE")
                        || c.equalsIgnoreCase("PHONE")));

        boolean hasWhatsapp = channels.stream()
                .anyMatch(c -> c != null && (c.equalsIgnoreCase("WHATSAPP") || c.equalsIgnoreCase("WA")));

        OtpUserDto.OtpUserDtoBuilder builder = OtpUserDto.builder()
                .id(user.getId());

        if (hasEmail) {
            builder.email(user.getEmail());
        }

        if (hasMobile) {
            builder.phone(user.getPhone());
        }

        if (hasWhatsapp) {
            builder.whatsapp(user.getWhatsapp());
        }

        if (hasMobile || hasWhatsapp) {
            builder.countryCode(user.getCountryCode());
        }

        return builder.build();
    }

    /**
     * Dispatches notification according to the chosen dispatch mode (WAY_1, WAY_2,
     * or BOTH), filtering user fields based on target channels.
     */
    public List<String> dispatchNotification(
            List<String> targetChannels,
            String secretKey,
            OtpUserDto user,
            String purposeId,
            String otp,
            long ttlSeconds,
            String dispatchModeOverride) {
        return dispatchNotification(targetChannels, secretKey, null, null, user, purposeId, otp, ttlSeconds,
                dispatchModeOverride, true);
    }

    /**
     * Dispatches notification according to the chosen dispatch mode (WAY_1, WAY_2,
     * or BOTH), with optional user field filtering.
     */
    public List<String> dispatchNotification(
            List<String> targetChannels,
            String secretKey,
            OtpUserDto user,
            String purposeId,
            String otp,
            long ttlSeconds,
            String dispatchModeOverride,
            boolean filterUser) {
        return dispatchNotification(targetChannels, secretKey, null, null, user, purposeId, otp, ttlSeconds,
                dispatchModeOverride, filterUser);
    }

    /**
     * Dispatches notification according to the chosen dispatch mode (WAY_1, WAY_2,
     * or BOTH), mentioning purpose in title and purpose name, project name, and OTP
     * in description.
     */
    public List<String> dispatchNotification(
            List<String> targetChannels,
            String secretKey,
            String projectName,
            String purposeName,
            OtpUserDto user,
            String purposeId,
            String otp,
            long ttlSeconds,
            String dispatchModeOverride,
            boolean filterUser) {

        // Skip dispatch entirely when no channels are configured
        if (targetChannels == null || targetChannels.isEmpty()) {
            log.info("No target channels configured — skipping notification dispatch (filterUser={}, dispatchMode={})",
                    filterUser, dispatchModeOverride);
            return List.of();
        }

        OtpUserDto userForDispatch = filterUser ? filterUserByChannels(user, targetChannels) : user;
        log.debug("User for dispatch based on channels {} (filterUser={}): email={}, phone={}, whatsapp={}",
                targetChannels,
                filterUser,
                userForDispatch != null ? userForDispatch.getEmail() : null,
                userForDispatch != null ? userForDispatch.getPhone() : null,
                userForDispatch != null ? userForDispatch.getWhatsapp() : null);

        String mode = (dispatchModeOverride != null && !dispatchModeOverride.trim().isEmpty())
                ? dispatchModeOverride.trim()
                : (notificationProperties != null ? notificationProperties.getDispatchMode() : "WAY_1");

        String title = otpNotificationTemplate.generateTitle(purposeName);
        String message = otpNotificationTemplate.generateDescription(purposeName, projectName, otp, ttlSeconds);

        if (mode == null) {
            mode = "WAY_1";
        }

        return switch (mode.toUpperCase()) {
            case "WAY_1", "INTERNAL_API", "REST", "HTTP" -> {
                List<String> dispatched = dispatchViaInternalApi(secretKey, userForDispatch, title, message,
                        targetChannels);
                // Fallback to legacy producer if client returned empty or is not configured
                if ((dispatched == null || dispatched.isEmpty()) && notificationServiceClient == null) {
                    if (projectName != null || purposeName != null) {
                        dispatched = notificationProducerService.dispatchToChannels(targetChannels, secretKey,
                                projectName, purposeName,
                                userForDispatch, purposeId, otp, ttlSeconds);
                    } else {
                        dispatched = notificationProducerService.dispatchToChannels(targetChannels, secretKey,
                                userForDispatch, purposeId, otp, ttlSeconds);
                    }
                }
                yield (dispatched != null && !dispatched.isEmpty()) ? dispatched : targetChannels;
            }
            case "WAY_2", "PRODUCER", "KAFKA", "DIRECT" -> {
                yield dispatchViaKafkaProducer(targetChannels, userForDispatch, title, message);
            }
            case "BOTH" -> {
                log.info("Dispatching notification via BOTH Way 1 and Way 2");
                List<String> r1 = dispatchViaInternalApi(secretKey, userForDispatch, title, message, targetChannels);
                List<String> r2 = dispatchViaKafkaProducer(targetChannels, userForDispatch, title, message);
                Set<String> combined = new LinkedHashSet<>();
                if (r1 != null)
                    combined.addAll(r1);
                if (r2 != null)
                    combined.addAll(r2);
                yield new ArrayList<>(combined.isEmpty() ? targetChannels : combined);
            }
            default -> {
                log.warn("Unknown dispatch mode '{}', using Way 1", mode);
                List<String> dispatched = dispatchViaInternalApi(secretKey, userForDispatch, title, message,
                        targetChannels);
                if ((dispatched == null || dispatched.isEmpty()) && notificationServiceClient == null) {
                    if (projectName != null || purposeName != null) {
                        dispatched = notificationProducerService.dispatchToChannels(targetChannels, secretKey,
                                projectName, purposeName,
                                userForDispatch, purposeId, otp, ttlSeconds);
                    } else {
                        dispatched = notificationProducerService.dispatchToChannels(targetChannels, secretKey,
                                userForDispatch, purposeId, otp, ttlSeconds);
                    }
                }
                yield (dispatched != null && !dispatched.isEmpty()) ? dispatched : targetChannels;
            }
        };
    }

    /**
     * Verifies the submitted OTP against Redis for the given secretKey and user.id.
     * If correct, deletes the OTP from Redis and completes successfully.
     */
    public void verifyOtp(OtpVerifyRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body cannot be null");
        }

        String secretKey = sanitize(request.getSecretKey());
        OtpUserDto user = request.getUser();
        String userId = user != null ? sanitize(user.getId()) : null;
        String otp = sanitize(request.getOtp());

        if (secretKey == null || secretKey.isEmpty()) {
            throw new IllegalArgumentException("Secret key is required");
        }
        if (user == null || userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("User and User ID are required");
        }
        if (otp == null || otp.isEmpty()) {
            throw new IllegalArgumentException("OTP is required");
        }

        String redisKey = buildKey(secretKey, userId);
        String storedOtp = redisService.get(redisKey);

        if (storedOtp == null || storedOtp.trim().isEmpty()) {
            log.warn("OTP verification failed — no active OTP found for key '{}'", redisKey);
            throw new IllegalArgumentException("OTP has expired or does not exist");
        }

        if (!storedOtp.trim().equals(otp.trim())) {
            log.warn("OTP verification mismatch for key '{}'", redisKey);
            throw new IllegalArgumentException("Invalid OTP");
        }

        // OTP is correct: delete from Redis
        redisService.delete(redisKey);
        log.info("OTP verified successfully and removed from Redis for key '{}'", redisKey);
    }

    private List<String> extractChannels(List<String> rawChannels) {
        if (rawChannels == null || rawChannels.isEmpty()) {
            return List.of();
        }
        return rawChannels.stream()
                .filter(c -> c != null && !c.trim().isEmpty())
                .map(String::trim)
                .toList();
    }

    private String buildKey(String secretKey, String userId) {
        return "otp:" + secretKey.trim() + ":" + userId.trim();
    }

    private String sanitize(String value) {
        return value != null ? value.trim() : null;
    }
}

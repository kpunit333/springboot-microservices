package orion.notification_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import orion.notification_service.service.impl.NotificationServiceImpl;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private NotificationProducer notificationProducer;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private UUID validSecretKey;
    private Project activeProject;
    private NotificationUserDto testUser;

    @BeforeEach
    void setUp() {
        validSecretKey = UUID.randomUUID();
        activeProject = Project.builder()
                .id(1L)
                .name("Test Project")
                .secretKey(validSecretKey)
                .status("ACTIVE")
                .channels(List.of(Channel.EMAIL, Channel.SMS, Channel.WHATSAPP))
                .createdAt(OffsetDateTime.now())
                .build();

        testUser = NotificationUserDto.builder()
                .id("usr_123")
                .email("test@example.com")
                .phone("9876543210")
                .countryCode("+91")
                .whatsapp("9876543210")
                .build();
    }

    @Test
    void testSendV1_success_allChannelsSubscribed() {
        when(projectRepository.findBySecretKey(validSecretKey)).thenReturn(Optional.of(activeProject));

        NotificationSendRequest request = NotificationSendRequest.builder()
                .secretKey(validSecretKey.toString())
                .user(testUser)
                .title("Welcome Alert")
                .description("Your account is now ready")
                .build();

        NotificationSendResponse response = notificationService.sendV1(request);

        assertNotNull(response);
        assertEquals("SENT", response.getStatus());
        assertEquals(3, response.getCount());
        assertTrue(response.getDispatchedChannels().contains(Channel.EMAIL));
        assertTrue(response.getDispatchedChannels().contains(Channel.SMS));
        assertTrue(response.getDispatchedChannels().contains(Channel.WHATSAPP));

        // Verify Email Producer
        ArgumentCaptor<ProducerPayload> emailCaptor = ArgumentCaptor.forClass(ProducerPayload.class);
        verify(notificationProducer).sendEmail(emailCaptor.capture());
        assertEquals("test@example.com", emailCaptor.getValue().getReceiver());
        assertEquals("Welcome Alert", emailCaptor.getValue().getTitle());
        assertEquals("Your account is now ready", emailCaptor.getValue().getDescription());
        assertNotNull(emailCaptor.getValue().getTimestamp());

        // Verify SMS Producer
        ArgumentCaptor<ProducerPayload> smsCaptor = ArgumentCaptor.forClass(ProducerPayload.class);
        verify(notificationProducer).sendSms(smsCaptor.capture());
        assertEquals("+919876543210", smsCaptor.getValue().getReceiver());
        assertEquals("Welcome Alert", smsCaptor.getValue().getTitle());

        // Verify WhatsApp Producer
        ArgumentCaptor<ProducerPayload> waCaptor = ArgumentCaptor.forClass(ProducerPayload.class);
        verify(notificationProducer).sendWhatsApp(waCaptor.capture());
        assertEquals("+919876543210", waCaptor.getValue().getReceiver());
    }

    @Test
    void testSendV1_onlyEmailSubscribed() {
        activeProject.setChannels(List.of(Channel.EMAIL));
        when(projectRepository.findBySecretKey(validSecretKey)).thenReturn(Optional.of(activeProject));

        NotificationSendRequest request = NotificationSendRequest.builder()
                .secretKey(validSecretKey.toString())
                .user(testUser)
                .title("Email Only")
                .description("Checking single channel")
                .build();

        NotificationSendResponse response = notificationService.sendV1(request);

        assertEquals("SENT", response.getStatus());
        assertEquals(1, response.getCount());
        assertTrue(response.getDispatchedChannels().contains(Channel.EMAIL));

        verify(notificationProducer).sendEmail(any(ProducerPayload.class));
        verify(notificationProducer, never()).sendSms(any());
        verify(notificationProducer, never()).sendWhatsApp(any());
    }

    @Test
    void testSendV1_whatsAppFallbackToPhone() {
        testUser.setWhatsapp(null); // No whatsapp explicitly set, should fallback to phone
        activeProject.setChannels(List.of(Channel.WHATSAPP));
        when(projectRepository.findBySecretKey(validSecretKey)).thenReturn(Optional.of(activeProject));

        NotificationSendRequest request = NotificationSendRequest.builder()
                .secretKey(validSecretKey.toString())
                .user(testUser)
                .title("WhatsApp Fallback")
                .description("Testing phone fallback")
                .build();

        NotificationSendResponse response = notificationService.sendV1(request);

        assertEquals("SENT", response.getStatus());
        ArgumentCaptor<ProducerPayload> waCaptor = ArgumentCaptor.forClass(ProducerPayload.class);
        verify(notificationProducer).sendWhatsApp(waCaptor.capture());
        assertEquals("+919876543210", waCaptor.getValue().getReceiver());
    }

    @Test
    void testSendV1_missingSecretKey_throwsInvalidSecretKeyException() {
        NotificationSendRequest request = NotificationSendRequest.builder()
                .secretKey(null)
                .user(testUser)
                .title("No Secret")
                .build();

        assertThrows(InvalidSecretKeyException.class, () -> notificationService.sendV1(request));
    }

    @Test
    void testSendV1_invalidUuid_throwsInvalidSecretKeyException() {
        NotificationSendRequest request = NotificationSendRequest.builder()
                .secretKey("not-a-uuid")
                .user(testUser)
                .title("Bad UUID")
                .build();

        assertThrows(InvalidSecretKeyException.class, () -> notificationService.sendV1(request));
    }

    @Test
    void testSendV1_projectNotFound_throwsInvalidSecretKeyException() {
        when(projectRepository.findBySecretKey(validSecretKey)).thenReturn(Optional.empty());

        NotificationSendRequest request = NotificationSendRequest.builder()
                .secretKey(validSecretKey.toString())
                .user(testUser)
                .title("Not Found")
                .build();

        assertThrows(InvalidSecretKeyException.class, () -> notificationService.sendV1(request));
    }

    @Test
    void testSendV1_inactiveProject_throwsProjectInactiveException() {
        activeProject.setStatus("INACTIVE");
        when(projectRepository.findBySecretKey(validSecretKey)).thenReturn(Optional.of(activeProject));

        NotificationSendRequest request = NotificationSendRequest.builder()
                .secretKey(validSecretKey.toString())
                .user(testUser)
                .title("Inactive Test")
                .build();

        assertThrows(ProjectInactiveException.class, () -> notificationService.sendV1(request));
    }

    @Test
    void testSendV1_expiredProject_throwsProjectExpiredException() {
        activeProject.setExpireAt(OffsetDateTime.now().minusDays(1));
        when(projectRepository.findBySecretKey(validSecretKey)).thenReturn(Optional.of(activeProject));

        NotificationSendRequest request = NotificationSendRequest.builder()
                .secretKey(validSecretKey.toString())
                .user(testUser)
                .title("Expired Test")
                .build();

        assertThrows(ProjectExpiredException.class, () -> notificationService.sendV1(request));
    }

    @Test
    void testSendInternal_doesNotVerifySecretKey_sendsToAvailableChannels() {
        // No secretKey provided at all
        NotificationSendRequest request = NotificationSendRequest.builder()
                .secretKey(null)
                .user(testUser)
                .title("Internal Notification")
                .description("Sending without secretKey check")
                .build();

        NotificationSendResponse response = notificationService.sendInternal(request);

        assertNotNull(response);
        assertEquals("SENT", response.getStatus());
        assertEquals(3, response.getCount());

        verify(notificationProducer).sendEmail(any(ProducerPayload.class));
        verify(notificationProducer).sendSms(any(ProducerPayload.class));
        verify(notificationProducer).sendWhatsApp(any(ProducerPayload.class));
    }

    @Test
    void testSendInternal_withValidSecretKey_usesProjectChannels() {
        activeProject.setChannels(List.of(Channel.SMS));
        when(projectRepository.findBySecretKey(validSecretKey)).thenReturn(Optional.of(activeProject));

        NotificationSendRequest request = NotificationSendRequest.builder()
                .secretKey(validSecretKey.toString())
                .user(testUser)
                .title("Internal Project Check")
                .build();

        NotificationSendResponse response = notificationService.sendInternal(request);

        assertEquals("SENT", response.getStatus());
        assertEquals(1, response.getCount());
        assertTrue(response.getDispatchedChannels().contains(Channel.SMS));

        verify(notificationProducer).sendSms(any(ProducerPayload.class));
        verify(notificationProducer, never()).sendEmail(any());
    }

    @Test
    void testSendInternal_withInvalidOrUnknownSecretKey_doesNotFail() {
        // Unknown secret key should not throw in internal endpoint
        UUID unknownKey = UUID.randomUUID();
        when(projectRepository.findBySecretKey(unknownKey)).thenReturn(Optional.empty());

        NotificationSendRequest request = NotificationSendRequest.builder()
                .secretKey(unknownKey.toString())
                .user(testUser)
                .title("Internal Unknown Key")
                .build();

        NotificationSendResponse response = notificationService.sendInternal(request);

        assertEquals("SENT", response.getStatus());
        assertEquals(3, response.getCount());
        verify(notificationProducer).sendEmail(any(ProducerPayload.class));
    }
}

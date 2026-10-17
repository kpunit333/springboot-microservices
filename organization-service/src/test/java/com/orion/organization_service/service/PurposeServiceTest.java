package com.orion.organization_service.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import com.orion.organization_service.dto.PurposeRequest;
import com.orion.organization_service.dto.PurposeResponse;
import com.orion.organization_service.model.Organization;
import com.orion.organization_service.model.Purpose;
import com.orion.organization_service.repository.OrganizationRepository;
import com.orion.organization_service.repository.PurposeRepository;

class PurposeServiceTest {

    @Mock
    private PurposeRepository purposeRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @InjectMocks
    private PurposeService purposeService;

    private Organization testOrg;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        testOrg = Organization.builder()
                .id(100L)
                .code("ORG123")
                .name("Test Org")
                .build();
    }

    @Test
    void testCreatePurposeSuccess() {
        when(organizationRepository.findByCode("ORG123")).thenReturn(Optional.of(testOrg));
        when(purposeRepository.save(any(Purpose.class))).thenAnswer(inv -> {
            Purpose p = inv.getArgument(0);
            p.setId(1L);
            p.prePersist();
            return p;
        });

        PurposeRequest request = PurposeRequest.builder()
                .name("Marketing Campaign")
                .description("Sending promo emails")
                .expireAt(OffsetDateTime.now().plusDays(30))
                .build();

        PurposeResponse response = purposeService.createForOrganization("ORG123", request);

        assertNotNull(response);
        assertEquals("Marketing Campaign", response.getName());
        assertEquals("Sending promo emails", response.getDescription());
        assertNotNull(response.getCode());
        assertTrue(response.getCode().startsWith("PUR"));
        verify(purposeRepository).save(any(Purpose.class));
    }

    @Test
    void testCreatePurposeEmptyDescriptionBecomesNull() {
        when(organizationRepository.findByCode("ORG123")).thenReturn(Optional.of(testOrg));
        when(purposeRepository.save(any(Purpose.class))).thenAnswer(inv -> {
            Purpose p = inv.getArgument(0);
            p.setId(2L);
            p.prePersist();
            return p;
        });

        PurposeRequest request = PurposeRequest.builder()
                .name("Two Factor Auth")
                .description("   ")
                .build();

        PurposeResponse response = purposeService.createForOrganization("ORG123", request);

        assertNotNull(response);
        assertEquals("Two Factor Auth", response.getName());
        assertNull(response.getDescription());
    }

    @Test
    void testCreatePurposeMissingNameThrows() {
        PurposeRequest request = PurposeRequest.builder()
                .description("No name")
                .build();

        assertThrows(IllegalArgumentException.class, () -> purposeService.createForOrganization("ORG123", request));
    }

    @Test
    void testGetPurposesByOrganizationSuccess() {
        when(organizationRepository.findByCode("ORG123")).thenReturn(Optional.of(testOrg));
        Purpose p1 = Purpose.builder()
                .id(1L)
                .organizationId(100L)
                .code("PUR001")
                .name("Purpose 1")
                .description("Desc 1")
                .createdAt(OffsetDateTime.now())
                .build();

        when(purposeRepository.findByOrganizationId(100L)).thenReturn(List.of(p1));

        List<PurposeResponse> result = purposeService.getPurposesByOrganization("ORG123");

        assertEquals(1, result.size());
        assertEquals("PUR001", result.get(0).getCode());
        assertEquals("Purpose 1", result.get(0).getName());
    }

    @Test
    void testGetByCodeAndOrganizationSuccess() {
        when(organizationRepository.findByCode("ORG123")).thenReturn(Optional.of(testOrg));
        Purpose p1 = Purpose.builder()
                .id(1L)
                .organizationId(100L)
                .code("PUR001")
                .name("Purpose 1")
                .description("Desc 1")
                .build();

        when(purposeRepository.findByCodeAndOrganizationId("PUR001", 100L)).thenReturn(Optional.of(p1));

        PurposeResponse response = purposeService.getByCodeAndOrganization("ORG123", "PUR001");

        assertNotNull(response);
        assertEquals("PUR001", response.getCode());
        assertEquals("Purpose 1", response.getName());
    }

    @Test
    void testGetByCodeAndOrganizationNotFound() {
        when(organizationRepository.findByCode("ORG123")).thenReturn(Optional.of(testOrg));
        when(purposeRepository.findByCodeAndOrganizationId("NONEXISTENT", 100L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> purposeService.getByCodeAndOrganization("ORG123", "NONEXISTENT"));
    }

    @Test
    void testUpdateForOrganizationSuccess() {
        when(organizationRepository.findByCode("ORG123")).thenReturn(Optional.of(testOrg));
        Purpose existing = Purpose.builder()
                .id(1L)
                .organizationId(100L)
                .code("PUR001")
                .name("Old Name")
                .description("Old Desc")
                .build();

        when(purposeRepository.findByCodeAndOrganizationId("PUR001", 100L)).thenReturn(Optional.of(existing));
        when(purposeRepository.save(any(Purpose.class))).thenAnswer(inv -> inv.getArgument(0));

        PurposeRequest updateRequest = PurposeRequest.builder()
                .name("New Name")
                .description("")
                .build();

        PurposeResponse response = purposeService.updateForOrganization("ORG123", "PUR001", updateRequest);

        assertEquals("New Name", response.getName());
        assertNull(response.getDescription());
    }

    @Test
    void testDeleteForOrganizationSuccess() {
        when(organizationRepository.findByCode("ORG123")).thenReturn(Optional.of(testOrg));
        Purpose existing = Purpose.builder()
                .id(1L)
                .organizationId(100L)
                .code("PUR001")
                .name("To Delete")
                .build();

        when(purposeRepository.findByCodeAndOrganizationId("PUR001", 100L)).thenReturn(Optional.of(existing));

        purposeService.deleteForOrganization("ORG123", "PUR001");

        verify(purposeRepository).delete(existing);
    }
}

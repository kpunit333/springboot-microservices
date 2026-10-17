package com.orion.organization_service.service;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import com.orion.organization_service.dto.OrganizationRequest;
import com.orion.organization_service.dto.OrganizationResponse;
import com.orion.organization_service.model.Organization;
import com.orion.organization_service.repository.OrganizationRepository;

class OrganizationServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private OrganizationService organizationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateOrganizationSuccess() {
        OrganizationRequest request = OrganizationRequest.builder()
                .name("Acme Corp")
                .email("admin@acme.com")
                .password("plainSecret")
                .build();

        when(organizationRepository.existsByName("Acme Corp")).thenReturn(false);
        when(organizationRepository.existsByEmail("admin@acme.com")).thenReturn(false);
        when(passwordEncoder.encode("plainSecret")).thenReturn("encodedSecret");
        when(organizationRepository.save(any(Organization.class))).thenAnswer(inv -> {
            Organization org = inv.getArgument(0);
            org.setId(10L);
            org.generateCode();
            return org;
        });

        OrganizationResponse response = organizationService.create(request);

        assertNotNull(response);
        assertEquals("Acme Corp", response.getName());
        assertEquals("admin@acme.com", response.getEmail());
        assertNotNull(response.getCode());
        assertTrue(response.getCode().startsWith("ORG"));
        verify(passwordEncoder).encode("plainSecret");
    }

    @Test
    void testCreateOrganizationValidationFailures() {
        // Missing name
        assertThrows(IllegalArgumentException.class, () -> 
            organizationService.create(OrganizationRequest.builder().email("admin@acme.com").password("secret").build())
        );

        // Missing email
        assertThrows(IllegalArgumentException.class, () -> 
            organizationService.create(OrganizationRequest.builder().name("Acme").password("secret").build())
        );

        // Missing password
        assertThrows(IllegalArgumentException.class, () -> 
            organizationService.create(OrganizationRequest.builder().name("Acme").email("admin@acme.com").build())
        );

        // Duplicate name
        when(organizationRepository.existsByName("Acme")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> 
            organizationService.create(OrganizationRequest.builder().name("Acme").email("admin@acme.com").password("secret").build())
        );

        // Duplicate email
        when(organizationRepository.existsByName("Unique Name")).thenReturn(false);
        when(organizationRepository.existsByEmail("duplicate@acme.com")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> 
            organizationService.create(OrganizationRequest.builder().name("Unique Name").email("duplicate@acme.com").password("secret").build())
        );
    }

    @Test
    void testUpdateOrganizationSuccessWithPasswordVerification() {
        Organization org = Organization.builder()
                .id(10L)
                .name("Old Name")
                .email("old@acme.com")
                .code("ORG123")
                .password("encodedSecret")
                .build();

        when(organizationRepository.findByCode("ORG123")).thenReturn(Optional.of(org));
        when(passwordEncoder.matches("currentSecret", "encodedSecret")).thenReturn(true);
        when(passwordEncoder.encode("newSecret")).thenReturn("encodedNewSecret");
        when(organizationRepository.save(any(Organization.class))).thenAnswer(inv -> inv.getArgument(0));

        OrganizationRequest request = OrganizationRequest.builder()
                .code("ORG123")
                .password("currentSecret")
                .name("New Name")
                .email("new@acme.com")
                .newPassword("newSecret")
                .build();

        OrganizationResponse response = organizationService.update(request);

        assertEquals("New Name", response.getName());
        assertEquals("new@acme.com", response.getEmail());
        assertEquals("ORG123", response.getCode());
        verify(passwordEncoder).matches("currentSecret", "encodedSecret");
        verify(passwordEncoder).encode("newSecret");
    }

    @Test
    void testUpdateOrganizationInvalidCredentials() {
        Organization org = Organization.builder()
                .id(10L)
                .name("Acme")
                .email("admin@acme.com")
                .code("ORG123")
                .password("encodedSecret")
                .build();

        when(organizationRepository.findByCode("ORG123")).thenReturn(Optional.of(org));
        when(passwordEncoder.matches("wrongSecret", "encodedSecret")).thenReturn(false);

        OrganizationRequest request = OrganizationRequest.builder()
                .code("ORG123")
                .password("wrongSecret")
                .name("New Name")
                .build();

        assertThrows(IllegalArgumentException.class, () -> organizationService.update(request));
    }

    @Test
    void testGetByEmailForAuthSuccess() {
        Organization org = Organization.builder()
                .id(10L)
                .name("Acme Corp")
                .email("admin@acme.com")
                .code("ORG123")
                .password("encodedSecret")
                .status("ACTIVE")
                .build();

        when(organizationRepository.findByEmailIgnoreCase("admin@acme.com")).thenReturn(Optional.of(org));

        var dto = organizationService.getByEmailForAuth("admin@acme.com");
        assertNotNull(dto);
        assertEquals("ORG123", dto.getCode());
        assertEquals("Acme Corp", dto.getName());
        assertEquals("admin@acme.com", dto.getEmail());
    }

    @Test
    void testGetByIdentifierForAuthByCode() {
        Organization org = Organization.builder()
                .id(10L)
                .name("Acme Corp")
                .email("admin@acme.com")
                .code("ORG123")
                .password("encodedSecret")
                .status("ACTIVE")
                .build();

        when(organizationRepository.findByCode("ORG123")).thenReturn(Optional.of(org));

        var dto = organizationService.getByIdentifierForAuth("ORG123");
        assertNotNull(dto);
        assertEquals("ORG123", dto.getCode());
        assertEquals("Acme Corp", dto.getName());
        assertEquals("admin@acme.com", dto.getEmail());
    }

    @Test
    void testGetByIdentifierForAuthByEmail() {
        Organization org = Organization.builder()
                .id(10L)
                .name("Acme Corp")
                .email("admin@acme.com")
                .code("ORG123")
                .password("encodedSecret")
                .status("ACTIVE")
                .build();

        when(organizationRepository.findByCode("admin@acme.com")).thenReturn(Optional.empty());
        when(organizationRepository.findByNameIgnoreCase("admin@acme.com")).thenReturn(Optional.empty());
        when(organizationRepository.findByName("admin@acme.com")).thenReturn(Optional.empty());
        when(organizationRepository.findByEmailIgnoreCase("admin@acme.com")).thenReturn(Optional.of(org));

        var dto = organizationService.getByIdentifierForAuth("admin@acme.com");
        assertNotNull(dto);
        assertEquals("ORG123", dto.getCode());
        assertEquals("admin@acme.com", dto.getEmail());
    }
}

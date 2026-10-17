package com.orion.organization_service.service;

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
import org.springframework.security.crypto.password.PasswordEncoder;

import com.orion.organization_service.dto.ProjectRequest;
import com.orion.organization_service.dto.ProjectResponse;
import com.orion.organization_service.model.Organization;
import com.orion.organization_service.model.Project;
import com.orion.organization_service.repository.OrganizationRepository;
import com.orion.organization_service.repository.ProjectRepository;

class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(passwordEncoder.encode(any())).thenAnswer(inv -> "encoded_" + inv.getArgument(0));
    }

    @Test
    void testCreateProjectSuccessWithOrganizationCodeLookup() {
        Organization org = Organization.builder()
                .id(42L)
                .code("ORG_ALPHA")
                .name("Alpha Org")
                .build();

        when(organizationRepository.findByCode("ORG_ALPHA")).thenReturn(Optional.of(org));
        when(projectRepository.existsByEmail("super@test.com")).thenReturn(false);
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> {
            Project p = inv.getArgument(0);
            p.setId(101L);
            p.prePersist();
            return p;
        });

        ProjectRequest request = ProjectRequest.builder()
                .name("Super Project")
                .email("super@test.com")
                .password("superPass123")
                .description("Sample description")
                .build();

        ProjectResponse response = projectService.create("ORG_ALPHA", request);

        assertNotNull(response);
        assertEquals("Super Project", response.getName());
        assertEquals("super@test.com", response.getEmail());
        assertEquals("Sample description", response.getDescription());
        assertNotNull(response.getCode());
        assertTrue(response.getCode().startsWith("PRO"));
        verify(organizationRepository).findByCode("ORG_ALPHA");
        verify(passwordEncoder).encode("superPass123");
    }

    @Test
    void testCreateForOrganizationDirectSuccess() {
        Organization org = Organization.builder()
                .id(42L)
                .code("ORG_BETA")
                .name("Beta Org")
                .build();

        when(organizationRepository.findByCode("ORG_BETA")).thenReturn(Optional.of(org));
        when(projectRepository.existsByEmail("scoped@test.com")).thenReturn(false);
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> {
            Project p = inv.getArgument(0);
            p.setId(201L);
            p.prePersist();
            return p;
        });

        ProjectRequest request = ProjectRequest.builder()
                .name("Scoped Project")
                .email("scoped@test.com")
                .password("pass123")
                .description("Desc")
                .build();

        ProjectResponse response = projectService.createForOrganization("ORG_BETA", request);

        assertNotNull(response);
        assertEquals("Scoped Project", response.getName());
        assertEquals("scoped@test.com", response.getEmail());
        assertNotNull(response.getCode());
        assertTrue(response.getCode().startsWith("PRO"));
    }

    @Test
    void testCreateProjectMissingInputs() {
        // Missing body
        assertThrows(IllegalArgumentException.class, () ->
            projectService.create("ORG1", null)
        );

        // Missing name
        assertThrows(IllegalArgumentException.class, () -> 
            projectService.create("ORG1", ProjectRequest.builder().email("e@test.com").password("p").build())
        );

        // Missing email
        assertThrows(IllegalArgumentException.class, () -> 
            projectService.create("ORG1", ProjectRequest.builder().name("Test").password("p").build())
        );

        // Missing password
        assertThrows(IllegalArgumentException.class, () -> 
            projectService.create("ORG1", ProjectRequest.builder().name("Test").email("e@test.com").build())
        );

        // Missing organizationCode
        assertThrows(IllegalArgumentException.class, () -> 
            projectService.create(null, ProjectRequest.builder().name("Test").email("e@test.com").password("p").build())
        );
    }

    @Test
    void testCreateProjectDuplicateEmail() {
        Organization org = Organization.builder().id(1L).code("ORG1").build();
        when(organizationRepository.findByCode("ORG1")).thenReturn(Optional.of(org));
        when(projectRepository.existsByEmail("existing@test.com")).thenReturn(true);

        ProjectRequest request = ProjectRequest.builder()
                .name("New Project")
                .email("existing@test.com")
                .password("pass123")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            projectService.create("ORG1", request)
        );
        assertTrue(ex.getMessage().contains("already exists with email"));
    }

    @Test
    void testGetProjectsByOrganization() {
        Organization org = Organization.builder().id(42L).code("ORG_ALPHA").build();
        Project p1 = Project.builder().id(1L).code("PRO1").name("P1").email("p1@test.com").organizationId(42L).build();
        Project p2 = Project.builder().id(2L).code("PRO2").name("P2").email("p2@test.com").organizationId(42L).build();

        when(organizationRepository.findByCode("ORG_ALPHA")).thenReturn(Optional.of(org));
        when(projectRepository.findByOrganizationId(42L)).thenReturn(List.of(p1, p2));

        List<ProjectResponse> list = projectService.getProjectsByOrganization("ORG_ALPHA");

        assertEquals(2, list.size());
        assertEquals("PRO1", list.get(0).getCode());
        assertEquals("p1@test.com", list.get(0).getEmail());
        assertEquals("PRO2", list.get(1).getCode());
    }

    @Test
    void testGetByCodeAndOrganizationSuccess() {
        Organization org = Organization.builder().id(42L).code("ORG_ALPHA").build();
        Project p = Project.builder().id(1L).code("PRO1").name("P1").email("p1@test.com").organizationId(42L).build();

        when(organizationRepository.findByCode("ORG_ALPHA")).thenReturn(Optional.of(org));
        when(projectRepository.findByCodeAndOrganizationId("PRO1", 42L)).thenReturn(Optional.of(p));

        ProjectResponse response = projectService.getByCodeAndOrganization("ORG_ALPHA", "PRO1");

        assertNotNull(response);
        assertEquals("PRO1", response.getCode());
        assertEquals("P1", response.getName());
        assertEquals("p1@test.com", response.getEmail());
    }

    @Test
    void testUpdateForOrganizationSuccess() {
        Organization org = Organization.builder().id(42L).code("ORG_ALPHA").build();
        Project existing = Project.builder().id(1L).code("PRO1").name("Old Name").email("old@test.com").organizationId(42L).build();

        when(organizationRepository.findByCode("ORG_ALPHA")).thenReturn(Optional.of(org));
        when(projectRepository.findByCodeAndOrganizationId("PRO1", 42L)).thenReturn(Optional.of(existing));
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

        ProjectRequest updateReq = ProjectRequest.builder().name("New Name").email("new@test.com").build();
        ProjectResponse response = projectService.updateForOrganization("ORG_ALPHA", "PRO1", updateReq);

        assertEquals("New Name", response.getName());
        assertEquals("new@test.com", response.getEmail());
    }

    @Test
    void testDeleteForOrganizationSuccess() {
        Organization org = Organization.builder().id(42L).code("ORG_ALPHA").build();
        Project existing = Project.builder().id(1L).code("PRO1").name("P1").organizationId(42L).build();

        when(organizationRepository.findByCode("ORG_ALPHA")).thenReturn(Optional.of(org));
        when(projectRepository.findByCodeAndOrganizationId("PRO1", 42L)).thenReturn(Optional.of(existing));

        projectService.deleteForOrganization("ORG_ALPHA", "PRO1");

        verify(projectRepository).delete(existing);
    }

    @Test
    void testCreateProjectOrganizationNotFound() {
        when(organizationRepository.findByCode("UNKNOWN_ORG")).thenReturn(Optional.empty());

        ProjectRequest request = ProjectRequest.builder()
                .name("Super Project")
                .email("super@test.com")
                .password("pass")
                .build();

        assertThrows(IllegalArgumentException.class, () -> projectService.create("UNKNOWN_ORG", request));
    }

    @Test
    void testUpdateProjectSuccess() {
        Project existing = Project.builder()
                .id(5L)
                .organizationId(42L)
                .code("PRO123")
                .name("Old Name")
                .email("old@test.com")
                .description("Old Desc")
                .build();

        when(projectRepository.findByCode("PRO123")).thenReturn(Optional.of(existing));
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

        ProjectRequest updateRequest = ProjectRequest.builder()
                .code("PRO123")
                .name("Updated Name")
                .description("Updated Desc")
                .build();

        ProjectResponse response = projectService.update(updateRequest);

        assertEquals("Updated Name", response.getName());
        assertEquals("Updated Desc", response.getDescription());
        assertEquals("PRO123", response.getCode());
    }

    @Test
    void testUpdateProjectMissingCode() {
        ProjectRequest request = ProjectRequest.builder()
                .name("Test")
                .build();

        assertThrows(IllegalArgumentException.class, () -> projectService.update(request));
    }

    @Test
    void testCreateProjectEmptyDescriptionBecomesNull() {
        Organization org = Organization.builder()
                .id(42L)
                .code("ORG_DESC")
                .name("Desc Org")
                .build();

        when(organizationRepository.findByCode("ORG_DESC")).thenReturn(Optional.of(org));
        when(projectRepository.existsByEmail("nodesc@test.com")).thenReturn(false);
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

        ProjectRequest request = ProjectRequest.builder()
                .name("No Desc Project")
                .email("nodesc@test.com")
                .password("pass123")
                .description("   ")
                .build();

        ProjectResponse response = projectService.createForOrganization("ORG_DESC", request);

        assertNotNull(response);
        assertNull(response.getDescription());
    }

    @Test
    void testUpdateProjectEmptyDescriptionBecomesNull() {
        Organization org = Organization.builder()
                .id(42L)
                .code("ORG_DESC")
                .name("Desc Org")
                .build();

        Project existing = Project.builder()
                .id(10L)
                .organizationId(42L)
                .code("PRO_DESC")
                .name("Initial")
                .description("Existing desc")
                .build();

        when(organizationRepository.findByCode("ORG_DESC")).thenReturn(Optional.of(org));
        when(projectRepository.findByCodeAndOrganizationId("PRO_DESC", 42L)).thenReturn(Optional.of(existing));
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

        ProjectRequest request = ProjectRequest.builder()
                .description("")
                .build();

        ProjectResponse response = projectService.updateForOrganization("ORG_DESC", "PRO_DESC", request);

        assertNotNull(response);
        assertNull(response.getDescription());
    }
}

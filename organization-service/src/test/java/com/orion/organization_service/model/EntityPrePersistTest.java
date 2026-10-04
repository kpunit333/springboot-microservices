package com.orion.organization_service.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class EntityPrePersistTest {

    @Test
    void testOrganizationPrePersistGeneratesCode() {
        Organization org = Organization.builder()
                .name("Test Org")
                .password("encodedPassword")
                .build();

        org.generateCode();

        assertNotNull(org.getCode());
        assertTrue(org.getCode().startsWith("ORG"));
        assertEquals(17, org.getCode().length());
    }

    @Test
    void testProjectPrePersistGeneratesCodeAndSecretKey() {
        Project project = Project.builder()
                .organizationId(1L)
                .name("Test Project")
                .build();

        project.prePersist();

        assertNotNull(project.getCode());
        assertTrue(project.getCode().startsWith("PRO"));
        assertEquals(17, project.getCode().length());
        assertNotNull(project.getSecretKey());
    }
}

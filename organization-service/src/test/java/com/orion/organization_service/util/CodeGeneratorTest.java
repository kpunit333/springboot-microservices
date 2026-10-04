package com.orion.organization_service.util;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class CodeGeneratorTest {

    @Test
    void testGenerateOrganizationCodeWithFixedDateTime() {
        // Nano: 450_000_000 ns = 450 ms -> 45 centiseconds (2 digit millis)
        LocalDateTime dt = LocalDateTime.of(2026, 9, 20, 21, 15, 30, 450_000_000);
        String code = CodeGenerator.generateCode("ORG", dt);

        // Date=20 -> 14, Month=9 -> 9, Year=2026 -> 07EA, Hour=21 -> V, Minute=15 -> 0F, Second=30 -> 1E, Millis=45
        assertEquals("ORG14907EAV0F1E45", code);
        assertEquals(17, code.length());
        assertTrue(code.startsWith("ORG"));
    }

    @Test
    void testGenerateProjectCodeWithFixedDateTime() {
        LocalDateTime dt = LocalDateTime.of(2026, 9, 20, 21, 15, 30, 450_000_000);
        String code = CodeGenerator.generateCode("PRO", dt);

        assertEquals("PRO14907EAV0F1E45", code);
        assertEquals(17, code.length());
        assertTrue(code.startsWith("PRO"));
    }

    @Test
    void testGenerateCodeWithBoundaryValues() {
        LocalDateTime dt = LocalDateTime.of(2025, 1, 5, 8, 4, 2, 70_000_000);
        String code = CodeGenerator.generateCode("ORG", dt);

        // Date=5 -> 05, Month=1 -> 1, Year=2025 -> 07E9, Hour=8 -> I, Minute=4 -> 04, Second=2 -> 02, Millis=07
        assertEquals("ORG05107E9I040207", code);
        assertEquals(17, code.length());
    }

    @Test
    void testGenerateCodeWithEndOfYearAndHourX() {
        LocalDateTime dt = LocalDateTime.of(2024, 12, 31, 23, 59, 59, 990_000_000);
        String code = CodeGenerator.generateCode("PRO", dt);

        // Date=31 -> 1F, Month=12 -> C, Year=2024 -> 07E8, Hour=23 -> X, Minute=59 -> 3B, Second=59 -> 3B, Millis=99
        assertEquals("PRO1FC07E8X3B3B99", code);
        assertEquals(17, code.length());
    }

    @Test
    void testGenerateCodeWithMidnightHourA() {
        LocalDateTime dt = LocalDateTime.of(2026, 10, 1, 0, 0, 0, 0);
        String code = CodeGenerator.generateCode("ORG", dt);

        // Date=1 -> 01, Month=10 -> A, Year=2026 -> 07EA, Hour=0 -> A, Minute=0 -> 00, Second=0 -> 00, Millis=00
        assertEquals("ORG01A07EAA000000", code);
        assertEquals(17, code.length());
    }

    @Test
    void testGenerateCurrentCode() {
        String orgCode = CodeGenerator.generateOrganizationCode();
        String proCode = CodeGenerator.generateProjectCode();

        assertNotNull(orgCode);
        assertNotNull(proCode);
        assertTrue(orgCode.startsWith("ORG"));
        assertTrue(proCode.startsWith("PRO"));
        assertEquals(17, orgCode.length());
        assertEquals(17, proCode.length());
    }
}

package com.orion.organization_service.util;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StringListConverterTest {

    private StringListConverter converter;

    @BeforeEach
    void setUp() {
        converter = new StringListConverter();
    }

    @Test
    void testConvertToDatabaseColumn() {
        List<String> list = List.of("WHATSAPP", "SMS", "EMAIL");
        String json = converter.convertToDatabaseColumn(list);
        assertEquals("[\"WHATSAPP\",\"SMS\",\"EMAIL\"]", json);

        assertEquals("[]", converter.convertToDatabaseColumn(null));
        assertEquals("[]", converter.convertToDatabaseColumn(List.of()));
    }

    @Test
    void testConvertToEntityAttributeFromJson() {
        String json = "[\"WHATSAPP\",\"SMS\",\"EMAIL\"]";
        List<String> result = converter.convertToEntityAttribute(json);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(List.of("WHATSAPP", "SMS", "EMAIL"), result);
    }

    @Test
    void testConvertToEntityAttributeFromCommaSeparated() {
        String csv = "WHATSAPP, SMS, EMAIL";
        List<String> result = converter.convertToEntityAttribute(csv);

        assertNotNull(result);
        assertEquals(List.of("WHATSAPP", "SMS", "EMAIL"), result);
    }

    @Test
    void testConvertToEntityAttributeEmptyOrNull() {
        assertTrue(converter.convertToEntityAttribute(null).isEmpty());
        assertTrue(converter.convertToEntityAttribute("").isEmpty());
        assertTrue(converter.convertToEntityAttribute("   ").isEmpty());
        assertTrue(converter.convertToEntityAttribute("[]").isEmpty());
    }
}

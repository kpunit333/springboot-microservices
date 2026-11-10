package orion.notification_service.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StringListConverterTest {

    private StringListConverter converter;

    @BeforeEach
    void setUp() {
        converter = new StringListConverter();
    }

    @Test
    void testConvertToEntityAttribute_jsonArray() {
        List<String> list = converter.convertToEntityAttribute("[\"EMAIL\",\"SMS\",\"WHATSAPP\"]");
        assertNotNull(list);
        assertEquals(3, list.size());
        assertTrue(list.contains("EMAIL"));
        assertTrue(list.contains("SMS"));
        assertTrue(list.contains("WHATSAPP"));
    }

    @Test
    void testConvertToEntityAttribute_commaSeparated() {
        List<String> list = converter.convertToEntityAttribute("EMAIL, SMS, WHATSAPP");
        assertNotNull(list);
        assertEquals(3, list.size());
        assertTrue(list.contains("EMAIL"));
        assertTrue(list.contains("SMS"));
        assertTrue(list.contains("WHATSAPP"));
    }

    @Test
    void testConvertToEntityAttribute_nullOrEmpty() {
        assertTrue(converter.convertToEntityAttribute(null).isEmpty());
        assertTrue(converter.convertToEntityAttribute("").isEmpty());
        assertTrue(converter.convertToEntityAttribute("[]").isEmpty());
    }

    @Test
    void testConvertToDatabaseColumn_validList() {
        String dbData = converter.convertToDatabaseColumn(List.of("EMAIL", "SMS"));
        assertNotNull(dbData);
        assertTrue(dbData.contains("EMAIL"));
        assertTrue(dbData.contains("SMS"));
    }

    @Test
    void testConvertToDatabaseColumn_nullOrEmptyList() {
        assertEquals("[]", converter.convertToDatabaseColumn(null));
        assertEquals("[]", converter.convertToDatabaseColumn(List.of()));
    }
}


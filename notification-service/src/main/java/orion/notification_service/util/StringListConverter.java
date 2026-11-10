package orion.notification_service.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

/**
 * JPA AttributeConverter that maps a List<String> to a JSON array string in PostgreSQL
 * and parses it back to List<String>. Also supports comma-separated fallback.
 */
@Slf4j
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            log.warn("Failed to serialize channels list to JSON, falling back to comma-separated: {}", e.getMessage());
            return String.join(",", attribute);
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty() || "[]".equals(dbData.trim())) {
            return new ArrayList<>();
        }
        String trimmed = dbData.trim();
        if (trimmed.startsWith("[")) {
            try {
                List<String> parsed = OBJECT_MAPPER.readValue(trimmed, new TypeReference<List<String>>() {});
                return parsed != null ? new ArrayList<>(parsed) : new ArrayList<>();
            } catch (Exception e) {
                log.warn("Failed to parse channels JSON from database, trying comma-separated: {}", e.getMessage());
            }
        }
        return Arrays.stream(trimmed.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(ArrayList::new));
    }
}


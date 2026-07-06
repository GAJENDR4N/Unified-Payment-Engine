package io.gomobi.payment.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * Safe JSON serialization helper for logging/audit purposes - never throws,
 * falls back to a placeholder string on failure so a serialization bug
 * never takes down a request in flight just because of a log statement.
 */
@Slf4j
public final class JsonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonUtils() {
    }

    public static String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize object of type {} to JSON", value.getClass().getSimpleName(), e);
            return "<unserializable>";
        }
    }
}

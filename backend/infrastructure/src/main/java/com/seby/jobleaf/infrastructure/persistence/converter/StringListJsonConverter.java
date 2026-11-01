package com.seby.jobleaf.infrastructure.persistence.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/**
 * Stores a list of strings as a JSON array in a {@code TEXT} column (e.g. {@code ["Java", "SQL"]}).
 */
@Converter
public class StringListJsonConverter implements AttributeConverter<List<String>, String> {

    private static final Logger log = LoggerFactory.getLogger(StringListJsonConverter.class);

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    /**
     * {@inheritDoc}
     */
    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        return attribute == null ? null : JSON_MAPPER.writeValueAsString(attribute);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return JSON_MAPPER.readValue(dbData, STRING_LIST);
        } catch (JacksonException e) {
            log.error("Stored value is not a JSON array of strings: {}", dbData, e);
            throw new IllegalArgumentException("Stored value is not a JSON array of strings", e);
        }
    }
}

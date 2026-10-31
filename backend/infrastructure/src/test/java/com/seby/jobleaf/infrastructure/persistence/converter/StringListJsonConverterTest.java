package com.seby.jobleaf.infrastructure.persistence.converter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StringListJsonConverterTest {

    private final StringListJsonConverter converter = new StringListJsonConverter();

    @Test
    @DisplayName("Should serialize a list of strings to a JSON array")
    void convertToDatabaseColumn_writesJsonArray() {
        assertThat(converter.convertToDatabaseColumn(List.of("Java", "401(k)"))).isEqualTo("[\"Java\",\"401(k)\"]");
    }

    @Test
    @DisplayName("Should parse a JSON array stored as text into a list of strings")
    void convertToEntityAttribute_readsJsonArray() {
        assertThat(converter.convertToEntityAttribute("[\"Adobe XD\", \"UI/UX Design\"]"))
                .containsExactly("Adobe XD", "UI/UX Design");
    }

    @Test
    @DisplayName("Should keep null values as null in both directions")
    void nullValues_areKeptAsNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
        assertThat(converter.convertToEntityAttribute(" ")).isNull();
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when the stored value is not a JSON array")
    void convertToEntityAttribute_whenMalformed_throws() {
        assertThatThrownBy(() -> converter.convertToEntityAttribute("Java, SQL"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Stored value is not a JSON array of strings");
    }
}

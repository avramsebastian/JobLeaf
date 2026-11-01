package com.seby.jobleaf.domain.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class TextUtilityTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\t", "\n"})
    @DisplayName("Should return null for null, empty or blank values")
    void normalizeNullableText_whenNullOrBlank_returnsNull(String value) {
        assertThat(TextUtility.normalizeNullableText(value)).isNull();
    }

    @Test
    @DisplayName("Should trim surrounding whitespace and keep inner whitespace")
    void normalizeNullableText_trimsValue() {
        assertThat(TextUtility.normalizeNullableText("  John  Doe \n")).isEqualTo("John  Doe");
    }

    @Test
    @DisplayName("Should return the same value when there is nothing to trim")
    void normalizeNullableText_whenAlreadyNormalized_returnsSameValue() {
        assertThat(TextUtility.normalizeNullableText("Employer")).isEqualTo("Employer");
    }
}

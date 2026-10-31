package com.seby.jobleaf.domain.exceptions.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DomainValidationExceptionTest {

    @Test
    @DisplayName("Should build a required-field exception with message, key, field and args")
    void requiredField_buildsException() {
        DomainValidationException exception = DomainValidationException.requiredField("email", "Contact");

        assertThat(exception).isInstanceOf(LocalizedException.class);
        assertThat(exception.getMessage()).isEqualTo("Contact email is required");
        assertThat(exception.getMessageKey()).isEqualTo("error.validation.field.required");
        assertThat(exception.getField()).isEqualTo("email");
        assertThat(exception.getArgs()).containsExactly("email");
    }

    @Test
    @DisplayName("Should keep a custom message key")
    void constructor_keepsCustomMessageKey() {
        DomainValidationException exception = new DomainValidationException("error.custom", "name", "Name is invalid");

        assertThat(exception.getMessageKey()).isEqualTo("error.custom");
        assertThat(exception.getField()).isEqualTo("name");
        assertThat(exception.getMessage()).isEqualTo("Name is invalid");
    }
}

package com.seby.jobleaf.domain.exceptions.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceNotFoundExceptionTest {

    @Test
    @DisplayName("Should initialize messageKey, message, and arguments correctly")
    void shouldInitializeFieldsCorrectly() {
        // Given
        String key = "error.resource.not.found";
        String message = "Resource was not found";
        Object[] args = new Object[]{"User", 123};

        // When
        ResourceNotFoundException exception = new ResourceNotFoundException(key, message, args);

        // Then
        assertThat(exception.getMessageKey()).isEqualTo(key);
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getArgs()).containsExactly("User", 123);
    }

    @Test
    @DisplayName("Should handle null arguments safely and return empty array")
    void shouldHandleNullArgsSafely() {
        // Given
        String key = "error.not.found";
        String message = "Not found";

        // When
        ResourceNotFoundException exception = new ResourceNotFoundException(key, message, (Object[]) null);

        // Then
        assertThat(exception.getMessageKey()).isEqualTo(key);
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getArgs()).isEmpty();
    }

    @Test
    @DisplayName("Should return a cloned copy of args to preserve immutability")
    void shouldReturnClonedArgs() {
        // Given
        Object[] originalArgs = new Object[]{"InitialValue"};
        ResourceNotFoundException exception = new ResourceNotFoundException("key", "message", originalArgs);

        // When
        Object[] retrievedArgs = exception.getArgs();
        retrievedArgs[0] = "MutatedValue";

        // Then
        assertThat(exception.getArgs()[0]).isEqualTo("InitialValue");
    }
}

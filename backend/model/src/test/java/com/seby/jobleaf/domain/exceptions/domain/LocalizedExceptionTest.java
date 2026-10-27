package com.seby.jobleaf.domain.exceptions.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocalizedExceptionTest {

    private static class TestLocalizedException extends LocalizedException {
        private final String key;
        private final Object[] args;

        TestLocalizedException(String messageKey, String fallbackMessage, Object... args) {
            super(fallbackMessage);
            this.key = messageKey;
            this.args = args;
        }

        @Override
        public String getMessageKey() {
            return key;
        }

        @Override
        public Object[] getArgs() {
            return args != null ? args : super.getArgs();
        }
    }

    private static class DefaultArgsLocalizedException extends LocalizedException {
        DefaultArgsLocalizedException(String fallbackMessage) {
            super(fallbackMessage);
        }

        @Override
        public String getMessageKey() {
            return "default.key";
        }
    }

    @Test
    @DisplayName("Should return correct message and key from subclass")
    void shouldReturnCorrectMessageAndKey() {
        // Given
        String key = "test.error.key";
        String message = "Something went wrong";
        Object[] args = new Object[]{"arg1", 42};

        // When
        LocalizedException exception = new TestLocalizedException(key, message, args);

        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getMessageKey()).isEqualTo(key);
        assertThat(exception.getArgs()).containsExactly("arg1", 42);
    }

    @Test
    @DisplayName("Should return empty args by default")
    void shouldReturnEmptyArgsByDefault() {
        // Given
        String message = "Fallback message";

        // When
        LocalizedException exception = new DefaultArgsLocalizedException(message);

        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getMessageKey()).isEqualTo("default.key");
        assertThat(exception.getArgs()).isEmpty();
    }
}

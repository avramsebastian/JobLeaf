package com.seby.jobleaf.domain.exceptions.company;

import com.seby.jobleaf.domain.exceptions.domain.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyNotFoundExceptionTest {

    @Test
    @DisplayName("Should initialize CompanyNotFoundException with correct message and standard message key")
    void shouldInitializeWithCorrectMessageAndKey() {
        // Given
        String message = "Company with id 123 not found";

        // When
        CompanyNotFoundException exception = new CompanyNotFoundException(message);

        // Then
        assertThat(exception).isInstanceOf(ResourceNotFoundException.class);
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getMessageKey()).isEqualTo(CompanyNotFoundException.MESSAGE_KEY);
        assertThat(exception.getMessageKey()).isEqualTo("error.company.not.found");
        assertThat(exception.getArgs()).isEmpty();
    }
}

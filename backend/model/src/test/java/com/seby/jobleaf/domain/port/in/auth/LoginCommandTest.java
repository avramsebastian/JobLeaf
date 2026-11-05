package com.seby.jobleaf.domain.port.in.auth;

import com.seby.jobleaf.domain.exceptions.domain.DomainValidationException;
import com.seby.jobleaf.domain.port.in.auth.LoginUseCase.LoginCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class LoginCommandTest {

    @Test
    @DisplayName("Should trim and lower-case the email, and keep the password exactly as typed")
    void constructor_normalizesEmailOnly() {
        LoginCommand command = new LoginCommand("  Admin@Example.COM ", " pass word ");

        assertThat(command.email()).isEqualTo("admin@example.com");
        assertThat(command.password()).isEqualTo(" pass word ");
    }

    @Test
    @DisplayName("Should reject a blank email")
    void constructor_whenEmailIsBlank_throws() {
        DomainValidationException exception = catchThrowableOfType(DomainValidationException.class,
                () -> new LoginCommand("  ", "secret123"));

        assertThat(exception.getField()).isEqualTo("email");
    }

    @Test
    @DisplayName("Should reject an empty password")
    void constructor_whenPasswordIsEmpty_throws() {
        DomainValidationException exception = catchThrowableOfType(DomainValidationException.class,
                () -> new LoginCommand("admin@example.com", ""));

        assertThat(exception.getField()).isEqualTo("password");
    }

    @Test
    @DisplayName("Should never print the password")
    void toString_masksPassword() {
        assertThat(new LoginCommand("admin@example.com", "super-secret").toString())
                .doesNotContain("super-secret")
                .contains("admin@example.com", "***");
    }
}

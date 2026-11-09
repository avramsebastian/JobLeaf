package com.seby.jobleaf.domain.port.in.auth;

import com.seby.jobleaf.domain.exceptions.domain.DomainValidationException;
import com.seby.jobleaf.domain.port.in.auth.RegisterUseCase.RegisterCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class RegisterCommandTest {

    private static final String PASSWORD = "a-Long-unique-pass-42";

    private static String invalidField(Runnable constructor) {
        return catchThrowableOfType(DomainValidationException.class, constructor::run).getField();
    }

    @Test
    @DisplayName("Should trim the text fields, lower-case the email and keep the password as typed")
    void constructor_normalizesFields() {
        RegisterCommand command = new RegisterCommand(" Jane Doe ", " Jane@Example.COM ", " +40712345678 ", " " + PASSWORD);

        assertThat(command.name()).isEqualTo("Jane Doe");
        assertThat(command.email()).isEqualTo("jane@example.com");
        assertThat(command.mobileNumber()).isEqualTo("+40712345678");
        assertThat(command.password()).isEqualTo(" " + PASSWORD);
    }

    @Test
    @DisplayName("Should reject missing name, email and mobile number")
    void constructor_whenRequiredFieldMissing_throws() {
        assertThat(invalidField(() -> new RegisterCommand(" ", "jane@example.com", "+40712345678", PASSWORD))).isEqualTo("name");
        assertThat(invalidField(() -> new RegisterCommand("Jane", null, "+40712345678", PASSWORD))).isEqualTo("email");
        assertThat(invalidField(() -> new RegisterCommand("Jane", "jane@example.com", "", PASSWORD))).isEqualTo("mobileNumber");
    }

    @Test
    @DisplayName("Should reject a mobile number that is not in international format")
    void constructor_whenMobileNumberHasNoCountryCode_throws() {
        assertThat(invalidField(() -> new RegisterCommand("Jane", "jane@example.com", "0712345678", PASSWORD)))
                .isEqualTo("mobileNumber");
    }

    @Test
    @DisplayName("Should reject passwords shorter than 8 or longer than 72 characters")
    void constructor_whenPasswordLengthInvalid_throws() {
        assertThat(invalidField(() -> new RegisterCommand("Jane", "jane@example.com", "+40712345678", "short"))).isEqualTo("password");
        assertThat(invalidField(() -> new RegisterCommand("Jane", "jane@example.com", "+40712345678", "x".repeat(73)))).isEqualTo("password");
    }

    @Test
    @DisplayName("Should never print the password")
    void toString_masksPassword() {
        assertThat(new RegisterCommand("Jane", "jane@example.com", "+40712345678", PASSWORD).toString())
                .doesNotContain(PASSWORD).contains("***");
    }
}

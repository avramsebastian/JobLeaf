package com.seby.jobleaf.domain.port.in.contact;

import com.seby.jobleaf.domain.exceptions.domain.DomainValidationException;
import com.seby.jobleaf.domain.model.SystemActor;
import com.seby.jobleaf.domain.port.in.contact.CreateContactUseCase.CreateContactCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class CreateContactCommandTest {

    @Test
    @DisplayName("Should trim all text fields")
    void constructor_trimsTextFields() {
        // When
        CreateContactCommand command = new CreateContactCommand(
                UUID.randomUUID(), "  John ", " john@example.com ", " Employer ", " Subject ", "  Hello there  ");

        // Then
        assertThat(command.name()).isEqualTo("John");
        assertThat(command.email()).isEqualTo("john@example.com");
        assertThat(command.userType()).isEqualTo("Employer");
        assertThat(command.subject()).isEqualTo("Subject");
        assertThat(command.message()).isEqualTo("Hello there");
    }

    @Test
    @DisplayName("Should keep the given user id")
    void constructor_keepsUserId() {
        UUID userId = UUID.randomUUID();

        CreateContactCommand command = new CreateContactCommand(userId, "John", "j@e.co", "Employer", "S", "Message");

        assertThat(command.userId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("Should fall back to the system actor when no user id is given")
    void constructor_whenUserIdIsNull_usesSystemActor() {
        CreateContactCommand command = new CreateContactCommand(null, "John", "j@e.co", "Employer", "S", "Message");

        assertThat(command.userId()).isEqualTo(SystemActor.ID);
    }

    @Test
    @DisplayName("Should reject a blank name")
    void constructor_whenNameIsBlank_throws() {
        assertRequiredField(() -> new CreateContactCommand(null, "   ", "j@e.co", "Employer", "S", "Message"), "name");
    }

    @Test
    @DisplayName("Should reject a missing email")
    void constructor_whenEmailIsNull_throws() {
        assertRequiredField(() -> new CreateContactCommand(null, "John", null, "Employer", "S", "Message"), "email");
    }

    @Test
    @DisplayName("Should reject a blank user type")
    void constructor_whenUserTypeIsBlank_throws() {
        assertRequiredField(() -> new CreateContactCommand(null, "John", "j@e.co", "", "S", "Message"), "userType");
    }

    @Test
    @DisplayName("Should reject a blank subject")
    void constructor_whenSubjectIsBlank_throws() {
        assertRequiredField(() -> new CreateContactCommand(null, "John", "j@e.co", "Employer", " ", "Message"), "subject");
    }

    @Test
    @DisplayName("Should reject a missing message")
    void constructor_whenMessageIsNull_throws() {
        assertRequiredField(() -> new CreateContactCommand(null, "John", "j@e.co", "Employer", "S", null), "message");
    }

    private static void assertRequiredField(ThrowingSupplier<CreateContactCommand> constructor, String field) {
        DomainValidationException exception = catchThrowableOfType(DomainValidationException.class, constructor::get);

        assertThat(exception).isNotNull();
        assertThat(exception.getField()).isEqualTo(field);
        assertThat(exception.getMessage()).isEqualTo("Contact " + field + " is required");
        assertThat(exception.getMessageKey()).isEqualTo(DomainValidationException.REQUIRED_FIELD_MESSAGE_KEY);
    }
}

package com.seby.jobleaf.infrastructure.web.mapper;

import com.seby.jobleaf.apis.generated.model.CreateContactRequest;
import com.seby.jobleaf.apis.generated.model.CreateContactResponse;
import com.seby.jobleaf.domain.model.SystemActor;
import com.seby.jobleaf.domain.port.in.contact.CreateContactUseCase.CreateContactCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ContactWebMapperTest {

    private ContactWebMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ContactWebMapper();
    }

    private CreateContactRequest createRequest() {
        CreateContactRequest request = new CreateContactRequest();
        request.setName(" John Doe ");
        request.setEmail("john@example.com");
        request.setUserType("Employer");
        request.setSubject("Job Posting");
        request.setMessage("I have a question about posting jobs.");
        return request;
    }

    @Test
    @DisplayName("Should map the request and user id into a normalized create command")
    void toCreateContactCommand_mapsAllFields() {
        // Given
        UUID userId = UUID.randomUUID();

        // When
        CreateContactCommand command = mapper.toCreateContactCommand(userId, createRequest());

        // Then
        assertThat(command.userId()).isEqualTo(userId);
        assertThat(command.name()).isEqualTo("John Doe");
        assertThat(command.email()).isEqualTo("john@example.com");
        assertThat(command.userType()).isEqualTo("Employer");
        assertThat(command.subject()).isEqualTo("Job Posting");
        assertThat(command.message()).isEqualTo("I have a question about posting jobs.");
    }

    @Test
    @DisplayName("Should use the system actor when the request is anonymous")
    void toCreateContactCommand_whenUserIdIsNull_usesSystemActor() {
        CreateContactCommand command = mapper.toCreateContactCommand(null, createRequest());

        assertThat(command.userId()).isEqualTo(SystemActor.ID);
    }

    @Test
    @DisplayName("Should build the success response")
    void toCreateContactResponse_returnsSuccessStatus() {
        CreateContactResponse response = mapper.toCreateContactResponse();

        assertThat(response.getStatus()).isEqualTo("Request processed successfully!");
    }
}

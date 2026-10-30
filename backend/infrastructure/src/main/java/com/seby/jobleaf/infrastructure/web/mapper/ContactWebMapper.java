package com.seby.jobleaf.infrastructure.web.mapper;

import com.seby.jobleaf.apis.generated.model.CreateContactRequest;
import com.seby.jobleaf.apis.generated.model.CreateContactResponse;
import com.seby.jobleaf.domain.model.Contact;
import com.seby.jobleaf.domain.port.in.contact.CreateContactUseCase;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Maps between domain {@link Contact} aggregates and OpenAPI contact DTOs.
 */
@Component
public class ContactWebMapper {

    public CreateContactUseCase.CreateContactCommand toCreateContactCommand(UUID userId, CreateContactRequest request){
        return new CreateContactUseCase.CreateContactCommand(
                userId,
                request.getName(),
                request.getEmail(),
                request.getUserType(),
                request.getSubject(),
                request.getMessage()
        );
    }

    public CreateContactResponse toCreateContactResponse() {
        var response = new CreateContactResponse();
        response.setStatus("Request processed successfully!");
        return response;
    }
}

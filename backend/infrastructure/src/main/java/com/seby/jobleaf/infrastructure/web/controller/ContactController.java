package com.seby.jobleaf.infrastructure.web.controller;

import com.seby.jobleaf.apis.generated.ContactApi;
import com.seby.jobleaf.apis.generated.model.CreateContactRequest;
import com.seby.jobleaf.apis.generated.model.CreateContactResponse;
import com.seby.jobleaf.domain.port.in.contact.CreateContactUseCase;
import com.seby.jobleaf.infrastructure.web.mapper.ContactWebMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST adapter implementing generated {@link ContactApi}.
 */
@RestController
@RequiredArgsConstructor
public class ContactController implements ContactApi {

    private final CreateContactUseCase createContactUseCase;
    private final ContactWebMapper contactWebMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<CreateContactResponse> createContact(CreateContactRequest createContactRequest) {
        createContactUseCase.execute(
                contactWebMapper.toCreateContactCommand(
                        null,
                        createContactRequest));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(contactWebMapper.toCreateContactResponse());
    }
}

package com.seby.jobleaf.infrastructure.web.controller;

import com.seby.jobleaf.apis.generated.ContactApi;
import com.seby.jobleaf.apis.generated.model.ContactPageResponse;
import com.seby.jobleaf.apis.generated.model.ContactResponse;
import com.seby.jobleaf.apis.generated.model.CreateContactRequest;
import com.seby.jobleaf.apis.generated.model.CreateContactResponse;
import com.seby.jobleaf.domain.port.in.contact.CloseContactUseCase;
import com.seby.jobleaf.domain.port.in.contact.CreateContactUseCase;
import com.seby.jobleaf.domain.port.in.contact.ReadContactUseCase;
import com.seby.jobleaf.infrastructure.web.mapper.ContactWebMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST adapter implementing generated {@link ContactApi}.
 * <p>
 * The {@code /admin} endpoints need {@code ROLE_ADMIN}: that is enforced by the security filter chain
 * ({@code adminPaths} in {@code PathsConfig}) before a request reaches this class.
 */
@RestController
@RequiredArgsConstructor
public class ContactController implements ContactApi {

    private final CreateContactUseCase createContactUseCase;
    private final ReadContactUseCase readContactUseCase;
    private final CloseContactUseCase closeContactUseCase;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<List<ContactResponse>> fetchNewContactMsgs() {
        var contacts = readContactUseCase.fetchNewContacts();
        return ResponseEntity.ok(contactWebMapper.toContactResponses(contacts));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<List<ContactResponse>> fetchNewContactMsgsWithSort(String sortBy, String sortDir) {
        var contacts = readContactUseCase.fetchNewContacts(contactWebMapper.toContactSort(sortBy, sortDir));
        return ResponseEntity.ok(contactWebMapper.toContactResponses(contacts));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<ContactPageResponse> fetchNewContactMsgsWithPaginationAndSort(Integer pageNumber,
                                                                                         Integer pageSize,
                                                                                         String sortBy,
                                                                                         String sortDir) {
        var page = readContactUseCase.fetchNewContacts(contactWebMapper.toContactSort(sortBy, sortDir),
                contactWebMapper.toPageQuery(pageNumber, pageSize));
        return ResponseEntity.ok(contactWebMapper.toContactPageResponse(page));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<String> closeContactMsg(UUID id) {
        closeContactUseCase.closeContact(id);
        return ResponseEntity.ok(contactWebMapper.toCloseContactResponse());
    }
}

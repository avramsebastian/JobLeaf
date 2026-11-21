package com.seby.jobleaf.infrastructure.web.mapper;

import com.seby.jobleaf.apis.generated.model.ContactPageResponse;
import com.seby.jobleaf.apis.generated.model.ContactResponse;
import com.seby.jobleaf.apis.generated.model.ContactStatus;
import com.seby.jobleaf.apis.generated.model.CreateContactRequest;
import com.seby.jobleaf.apis.generated.model.CreateContactResponse;
import com.seby.jobleaf.domain.model.contact.Contact;
import com.seby.jobleaf.domain.model.contact.ContactSort;
import com.seby.jobleaf.domain.model.page.PageQuery;
import com.seby.jobleaf.domain.model.page.PageResult;
import com.seby.jobleaf.domain.port.in.contact.CreateContactUseCase;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Maps between domain {@link Contact} aggregates and OpenAPI contact DTOs.
 */
@Component
public class ContactWebMapper {

    static final String CLOSE_SUCCESS_MESSAGE = "Contact message closed successfully";

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

    /**
     * Turns the raw query parameters into the domain sort; invalid values become a 400.
     */
    public ContactSort toContactSort(String sortBy, String sortDir) {
        return ContactSort.of(sortBy, sortDir);
    }

    public PageQuery toPageQuery(Integer pageNumber, Integer pageSize) {
        return PageQuery.of(pageNumber, pageSize);
    }

    public ContactResponse toContactResponse(Contact contact) {
        var response = new ContactResponse();
        response.setId(contact.id());
        response.setName(contact.name());
        response.setEmail(contact.email());
        response.setUserType(contact.userType());
        response.setSubject(contact.subject());
        response.setMessage(contact.message());
        response.setStatus(ContactStatus.fromValue(contact.status().name()));
        response.setCreatedAt(toOffsetDateTime(contact.createdAt()));
        response.setUpdatedAt(toOffsetDateTime(contact.updatedAt()));
        return response;
    }

    public List<ContactResponse> toContactResponses(List<Contact> contacts) {
        return contacts.stream().map(this::toContactResponse).toList();
    }

    public ContactPageResponse toContactPageResponse(PageResult<Contact> page) {
        var response = new ContactPageResponse();
        response.setContent(toContactResponses(page.content()));
        response.setPageNumber(page.pageNumber());
        response.setPageSize(page.pageSize());
        response.setTotalElements(page.totalElements());
        response.setTotalPages(page.totalPages());
        response.setFirst(page.first());
        response.setLast(page.last());
        return response;
    }

    public String toCloseContactResponse() {
        return CLOSE_SUCCESS_MESSAGE;
    }

    private static OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}

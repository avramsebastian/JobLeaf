package com.seby.jobleaf.infrastructure.web.controller;

import com.seby.jobleaf.domain.exceptions.contact.ContactNotFoundException;
import com.seby.jobleaf.domain.model.Contact;
import com.seby.jobleaf.domain.model.ContactSort;
import com.seby.jobleaf.domain.model.ContactSortField;
import com.seby.jobleaf.domain.model.ContactStatus;
import com.seby.jobleaf.domain.model.page.PageQuery;
import com.seby.jobleaf.domain.model.page.PageResult;
import com.seby.jobleaf.domain.model.page.SortDirection;
import com.seby.jobleaf.domain.port.in.contact.CloseContactUseCase;
import com.seby.jobleaf.domain.port.in.contact.CreateContactUseCase;
import com.seby.jobleaf.domain.port.in.contact.ReadContactUseCase;
import com.seby.jobleaf.infrastructure.web.mapper.ContactWebMapper;
import com.seby.jobleaf.infrastructure.web.security.AccessTokenCookies;
import com.seby.jobleaf.infrastructure.web.security.JobLeafEmailPwdAuthenticationProvider;
import com.seby.jobleaf.infrastructure.web.security.JobLeafSecurityConfig;
import com.seby.jobleaf.infrastructure.web.security.PathsConfig;
import com.seby.jobleaf.infrastructure.web.security.ProblemDetailSecurityHandler;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs the admin contact endpoints through the real security filter chain: who may call them (401 / 403 / 200),
 * CSRF on the PATCH, parameter defaults and validation, and the response shapes.
 */
@WebMvcTest(ContactController.class)
@Import({JobLeafSecurityConfig.class, PathsConfig.class, ProblemDetailSecurityHandler.class, AccessTokenCookies.class,
        ContactWebMapper.class})
class ContactAdminWebTest {

    private static final String API_ACCEPT = "application/vnd.jobleaf+json;v=1.0";
    private static final UUID CONTACT_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final Contact CONTACT = new Contact(CONTACT_ID, "Jane Doe", "jane@example.com", "Employer",
            "Subject", "A message long enough", ContactStatus.NEW, Instant.parse("2026-11-15T10:00:00Z"),
            UUID.randomUUID(), null, null);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecretKey jwtSigningKey;

    @MockitoBean
    private JobLeafEmailPwdAuthenticationProvider authenticationProvider;

    @MockitoBean
    private CreateContactUseCase createContactUseCase;

    @MockitoBean
    private ReadContactUseCase readContactUseCase;

    @MockitoBean
    private CloseContactUseCase closeContactUseCase;

    private String bearer(String role) {
        return "Bearer " + Jwts.builder().issuer("jobleaf").subject(UUID.randomUUID().toString())
                .claim("email", "someone@example.com").claim("roles", role)
                .expiration(Date.from(Instant.now().plusSeconds(600))).signWith(jwtSigningKey).compact();
    }

    private static RequestPostProcessor csrfToken() {
        return request -> {
            request.setCookies(new Cookie("XSRF-TOKEN", "test-csrf-token"));
            request.addHeader("X-XSRF-TOKEN", "test-csrf-token");
            return request;
        };
    }

    @Test
    @DisplayName("Should answer admin endpoints without a token with 401")
    void adminEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/jobleaf/contacts/admin").accept(API_ACCEPT))
                .andExpect(status().isUnauthorized());
        verify(readContactUseCase, never()).fetchNewContacts();
    }

    @Test
    @DisplayName("Should answer admin endpoints for a logged in job seeker with 403")
    void adminEndpoint_asJobSeeker_returns403() throws Exception {
        mockMvc.perform(get("/jobleaf/contacts/admin").accept(API_ACCEPT)
                        .header("Authorization", bearer("ROLE_JOB_SEEKER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        verify(readContactUseCase, never()).fetchNewContacts();
    }

    @Test
    @DisplayName("Should list the new contact messages for an admin")
    void fetchNewContactMsgs_asAdmin_returnsList() throws Exception {
        when(readContactUseCase.fetchNewContacts()).thenReturn(List.of(CONTACT));

        mockMvc.perform(get("/jobleaf/contacts/admin").accept(API_ACCEPT).header("Authorization", bearer("ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(CONTACT_ID.toString()))
                .andExpect(jsonPath("$[0].name").value("Jane Doe"))
                .andExpect(jsonPath("$[0].status").value("NEW"))
                .andExpect(jsonPath("$[0].createdAt").exists());
    }

    @Test
    @DisplayName("Should default the sort to createdAt asc and pass explicit values on")
    void fetchNewContactMsgsWithSort_passesSort() throws Exception {
        when(readContactUseCase.fetchNewContacts(any(ContactSort.class))).thenReturn(List.of(CONTACT));

        mockMvc.perform(get("/jobleaf/contacts/sort/admin").accept(API_ACCEPT)
                        .header("Authorization", bearer("ROLE_ADMIN")))
                .andExpect(status().isOk());
        verify(readContactUseCase).fetchNewContacts(ContactSort.DEFAULT);

        mockMvc.perform(get("/jobleaf/contacts/sort/admin").accept(API_ACCEPT)
                        .header("Authorization", bearer("ROLE_ADMIN"))
                        .param("sortBy", "email").param("sortDir", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("jane@example.com"));
        verify(readContactUseCase).fetchNewContacts(new ContactSort(ContactSortField.EMAIL, SortDirection.DESC));
    }

    @Test
    @DisplayName("Should reject a sortBy that is not on the allow-list with a 400 naming the field")
    void fetchNewContactMsgsWithSort_unknownField_returns400() throws Exception {
        mockMvc.perform(get("/jobleaf/contacts/sort/admin").accept(API_ACCEPT)
                        .header("Authorization", bearer("ROLE_ADMIN")).param("sortBy", "password"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.sortBy").exists());
        verify(readContactUseCase, never()).fetchNewContacts(any(ContactSort.class));
    }

    @Test
    @DisplayName("Should default to page 0 of size 10 and return the page numbers with the content")
    void fetchNewContactMsgsWithPaginationAndSort_defaults() throws Exception {
        when(readContactUseCase.fetchNewContacts(ContactSort.DEFAULT, new PageQuery(0, 10)))
                .thenReturn(new PageResult<>(List.of(CONTACT), 0, 10, 11, 2));

        mockMvc.perform(get("/jobleaf/contacts/page/admin").accept(API_ACCEPT)
                        .header("Authorization", bearer("ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(CONTACT_ID.toString()))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.totalElements").value(11))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));
    }

    @Test
    @DisplayName("Should reject a page size above the maximum with a 400")
    void fetchNewContactMsgsWithPaginationAndSort_tooLargePage_returns400() throws Exception {
        mockMvc.perform(get("/jobleaf/contacts/page/admin").accept(API_ACCEPT)
                        .header("Authorization", bearer("ROLE_ADMIN")).param("pageSize", "1000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.pageSize").exists());
    }

    @Test
    @DisplayName("Should close a contact message for an admin with the CSRF token")
    void closeContactMsg_asAdmin_returnsMessage() throws Exception {
        when(closeContactUseCase.closeContact(CONTACT_ID)).thenReturn(CONTACT.close());

        mockMvc.perform(patch("/jobleaf/contacts/{id}/status/admin", CONTACT_ID).accept(API_ACCEPT)
                        .header("Authorization", bearer("ROLE_ADMIN")).with(csrfToken()))
                .andExpect(status().isOk())
                .andExpect(content().string("Contact message closed successfully"));
        verify(closeContactUseCase).closeContact(CONTACT_ID);
    }

    @Test
    @DisplayName("Should reject closing without the CSRF token, even for an admin")
    void closeContactMsg_withoutCsrf_returns403() throws Exception {
        mockMvc.perform(patch("/jobleaf/contacts/{id}/status/admin", CONTACT_ID).accept(API_ACCEPT)
                        .header("Authorization", bearer("ROLE_ADMIN")))
                .andExpect(status().isForbidden());
        verify(closeContactUseCase, never()).closeContact(any());
    }

    @Test
    @DisplayName("Should not let a job seeker close a contact message")
    void closeContactMsg_asJobSeeker_returns403() throws Exception {
        mockMvc.perform(patch("/jobleaf/contacts/{id}/status/admin", CONTACT_ID).accept(API_ACCEPT)
                        .header("Authorization", bearer("ROLE_JOB_SEEKER")).with(csrfToken()))
                .andExpect(status().isForbidden());
        verify(closeContactUseCase, never()).closeContact(any());
    }

    @Test
    @DisplayName("Should answer closing an unknown contact with 404")
    void closeContactMsg_unknownId_returns404() throws Exception {
        when(closeContactUseCase.closeContact(CONTACT_ID)).thenThrow(new ContactNotFoundException(CONTACT_ID));

        mockMvc.perform(patch("/jobleaf/contacts/{id}/status/admin", CONTACT_ID).accept(API_ACCEPT)
                        .header("Authorization", bearer("ROLE_ADMIN")).with(csrfToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should answer an id that is not a UUID with 400")
    void closeContactMsg_invalidId_returns400() throws Exception {
        mockMvc.perform(patch("/jobleaf/contacts/{id}/status/admin", "not-a-uuid").accept(API_ACCEPT)
                        .header("Authorization", bearer("ROLE_ADMIN")).with(csrfToken()))
                .andExpect(status().isBadRequest());
    }
}

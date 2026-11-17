package com.seby.jobleaf.infrastructure.web.controller;

import com.seby.jobleaf.domain.exceptions.company.CompanyAlreadyExistsException;
import com.seby.jobleaf.domain.exceptions.company.CompanyNotFoundException;
import com.seby.jobleaf.domain.model.Company;
import com.seby.jobleaf.domain.model.CompanyDetails;
import com.seby.jobleaf.domain.model.SystemActor;
import com.seby.jobleaf.domain.port.in.company.CreateCompanyUseCase;
import com.seby.jobleaf.domain.port.in.company.DeleteCompanyUseCase;
import com.seby.jobleaf.domain.port.in.company.UpdateCompanyUseCase;
import com.seby.jobleaf.domain.port.in.company.ReadCompanyUseCase;
import com.seby.jobleaf.infrastructure.web.mapper.CompanyWebMapper;
import com.seby.jobleaf.infrastructure.web.mapper.JobWebMapper;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The admin company endpoints through the real security chain, request validation and mappers.
 */
@WebMvcTest(CompanyController.class)
@Import({JobLeafSecurityConfig.class, PathsConfig.class, ProblemDetailSecurityHandler.class, AccessTokenCookies.class,
        CompanyWebMapper.class, JobWebMapper.class})
class CompanyAdminWebTest {

    private static final String API = "application/vnd.jobleaf+json;v=1.0";
    private static final UUID ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final String VALID_BODY = """
            {"name":"DerpCorp","logo":"/logos/d.png","industry":"Technology","size":"Large","rating":4.5,
             "locations":"Bucharest","founded":2010,"description":"A company","employees":100,
             "website":"https://d.example"}""";
    private static final Company COMPANY = Company.create(ID, new CompanyDetails("DerpCorp", "/logos/d.png",
            "Technology", "Large", 4.5, "Bucharest", 2010, "A company", 100, "https://d.example"), SystemActor.ID);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecretKey jwtSigningKey;

    @MockitoBean
    private JobLeafEmailPwdAuthenticationProvider authenticationProvider;

    @MockitoBean
    private ReadCompanyUseCase readCompanyUseCase;

    @MockitoBean
    private CreateCompanyUseCase createCompanyUseCase;

    @MockitoBean
    private UpdateCompanyUseCase updateCompanyUseCase;

    @MockitoBean
    private DeleteCompanyUseCase deleteCompanyUseCase;

    private String bearer(String role) {
        return "Bearer " + Jwts.builder().issuer("jobleaf").subject(UUID.randomUUID().toString())
                .claim("email", "someone@example.com").claim("roles", role)
                .expiration(Date.from(Instant.now().plusSeconds(600))).signWith(jwtSigningKey).compact();
    }

    private RequestPostProcessor admin() {
        return request -> {
            request.addHeader("Authorization", bearer("ROLE_ADMIN"));
            request.setCookies(new Cookie("XSRF-TOKEN", "test-csrf-token"));
            request.addHeader("X-XSRF-TOKEN", "test-csrf-token");
            return request;
        };
    }

    @Test
    @DisplayName("Should answer without token 401 and for a job seeker 403")
    void nonAdmins_rejected() throws Exception {
        mockMvc.perform(get("/jobleaf/companies/admin").accept(API)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/jobleaf/companies/admin").accept(API)
                        .header("Authorization", bearer("ROLE_JOB_SEEKER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should list companies for the admin without a jobs field")
    void listCompaniesForAdmin_withoutJobs() throws Exception {
        when(readCompanyUseCase.listCompaniesForAdmin()).thenReturn(List.of(COMPANY));

        mockMvc.perform(get("/jobleaf/companies/admin").accept(API).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("DerpCorp"))
                .andExpect(jsonPath("$[0].rating").value(4.5))
                .andExpect(jsonPath("$[0].jobs").doesNotExist());
    }

    @Test
    @DisplayName("Should create a company and answer 201")
    void createCompany_returns201() throws Exception {
        when(createCompanyUseCase.createCompany(any())).thenReturn(COMPANY);

        mockMvc.perform(post("/jobleaf/companies/admin").with(admin()).contentType(API).accept(API).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ID.toString()))
                .andExpect(jsonPath("$.jobs").doesNotExist());
    }

    @Test
    @DisplayName("Should answer invalid fields with 400 and the messages from the request annotations")
    void createCompany_invalid_returns400WithMessages() throws Exception {
        String body = """
                {"name":" ","logo":"/l.png","industry":"Tech","size":"S","rating":7,"locations":"B","founded":1800,
                 "description":"D","employees":0,"website":"https://w.example"}""";

        mockMvc.perform(post("/jobleaf/companies/admin").with(admin()).contentType(API).accept(API).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("Name can not be empty"))
                .andExpect(jsonPath("$.errors.rating").value("Rating must be at most 5"))
                .andExpect(jsonPath("$.errors.founded").value("Founded must be 1900 or later"))
                .andExpect(jsonPath("$.errors.employees").value("Employees must be greater than or equal to 1"));
        verify(createCompanyUseCase, never()).createCompany(any());
    }

    @Test
    @DisplayName("Should answer a taken name with 409 naming the field")
    void createCompany_duplicate_returns409() throws Exception {
        when(createCompanyUseCase.createCompany(any())).thenThrow(new CompanyAlreadyExistsException("DerpCorp"));

        mockMvc.perform(post("/jobleaf/companies/admin").with(admin()).contentType(API).accept(API).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    @DisplayName("Should update a company, and answer 404 for an unknown one")
    void updateCompany() throws Exception {
        when(updateCompanyUseCase.updateCompany(eq(ID), any())).thenReturn(COMPANY);
        mockMvc.perform(put("/jobleaf/companies/{id}/admin", ID).with(admin()).contentType(API).accept(API)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("DerpCorp"));

        UUID unknown = UUID.randomUUID();
        when(updateCompanyUseCase.updateCompany(eq(unknown), any()))
                .thenThrow(new CompanyNotFoundException("Company with id " + unknown + " not found"));
        mockMvc.perform(put("/jobleaf/companies/{id}/admin", unknown).with(admin()).contentType(API).accept(API)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should delete a company and answer with a message")
    void deleteCompany_returnsMessage() throws Exception {
        mockMvc.perform(delete("/jobleaf/companies/{id}/admin", ID).with(admin()).accept(API))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Company deleted successfully"));
        verify(deleteCompanyUseCase).deleteCompany(ID);
    }
}

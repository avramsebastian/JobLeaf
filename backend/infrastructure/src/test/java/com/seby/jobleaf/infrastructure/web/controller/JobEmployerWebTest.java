package com.seby.jobleaf.infrastructure.web.controller;

import com.seby.jobleaf.domain.exceptions.job.InvalidJobStatusChangeException;
import com.seby.jobleaf.domain.exceptions.job.JobNotFoundException;
import com.seby.jobleaf.domain.exceptions.user.EmployerAccessException;
import com.seby.jobleaf.domain.model.job.Job;
import com.seby.jobleaf.domain.model.job.JobDetails;
import com.seby.jobleaf.domain.model.job.JobStatus;
import com.seby.jobleaf.domain.model.user.SystemActor;
import com.seby.jobleaf.domain.port.in.job.CreateJobUseCase;
import com.seby.jobleaf.domain.port.in.job.ListEmployerJobsUseCase;
import com.seby.jobleaf.domain.port.in.job.UpdateJobStatusUseCase;
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
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The employer job endpoints through the real security chain, request validation and mappers.
 */
@WebMvcTest(JobController.class)
@Import({JobLeafSecurityConfig.class, PathsConfig.class, ProblemDetailSecurityHandler.class, AccessTokenCookies.class,
        JobWebMapper.class})
class JobEmployerWebTest {

    private static final String API = "application/vnd.jobleaf+json;v=1.0";
    private static final UUID EMPLOYER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID COMPANY_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final String DEADLINE = Instant.now().plus(Duration.ofDays(30)).toString();
    private static final String VALID_BODY = """
            {"title":"Frontend Developer","location":"Bucharest","workType":"Hybrid","jobType":"Full-time",
             "category":"Technology","experienceLevel":"Mid Level","salaryMin":50000,"salaryMax":80000,
             "salaryCurrency":"EUR","salaryPeriod":"year","description":"We are looking for a frontend developer.",
             "requirements":["React"],"benefits":["Remote Work"],"applicationDeadline":"%s",
             "featured":false,"urgent":true,"remote":false}""".formatted(DEADLINE);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecretKey jwtSigningKey;

    @MockitoBean
    private JobLeafEmailPwdAuthenticationProvider authenticationProvider;

    @MockitoBean
    private ListEmployerJobsUseCase listEmployerJobsUseCase;

    @MockitoBean
    private CreateJobUseCase createJobUseCase;

    @MockitoBean
    private UpdateJobStatusUseCase updateJobStatusUseCase;

    private String bearer(String role) {
        return "Bearer " + Jwts.builder().issuer("jobleaf").subject(EMPLOYER_ID.toString())
                .claim("email", "emma@example.com").claim("roles", role)
                .expiration(Date.from(Instant.now().plusSeconds(600))).signWith(jwtSigningKey).compact();
    }

    private RequestPostProcessor as(String role) {
        return request -> {
            request.addHeader("Authorization", bearer(role));
            request.setCookies(new Cookie("XSRF-TOKEN", "test-csrf-token"));
            request.addHeader("X-XSRF-TOKEN", "test-csrf-token");
            return request;
        };
    }

    private static Job draft() {
        var details = new JobDetails("Frontend Developer", "Bucharest", "Hybrid", "Full-time", "Technology",
                "Mid Level", BigDecimal.valueOf(50_000), BigDecimal.valueOf(80_000), "EUR", "year",
                "We are looking for a frontend developer.", List.of("React"), List.of("Remote Work"),
                Instant.now().plus(Duration.ofDays(30)), false, true, false);
        return Job.create(UUID.randomUUID(), COMPANY_ID, details, Instant.now(), SystemActor.ID);
    }

    @Test
    @DisplayName("Should answer without token 401, and for job seekers and admins 403")
    void nonEmployers_rejected() throws Exception {
        mockMvc.perform(get("/jobleaf/jobs/employer").accept(API)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/jobleaf/jobs/employer").accept(API).with(as("ROLE_JOB_SEEKER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/jobleaf/jobs/employer").accept(API).with(as("ROLE_ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should list the jobs for the logged-in employer, whose id comes from the token")
    void list_usesTokenUser() throws Exception {
        when(listEmployerJobsUseCase.listJobs(EMPLOYER_ID)).thenReturn(List.of(draft()));

        mockMvc.perform(get("/jobleaf/jobs/employer").accept(API).with(as("ROLE_EMPLOYER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("DRAFT"))
                .andExpect(jsonPath("$[0].companyId").value(COMPANY_ID.toString()));
    }

    @Test
    @DisplayName("Should answer an employer without company with the 403 message from the domain")
    void list_noCompany_403() throws Exception {
        when(listEmployerJobsUseCase.listJobs(EMPLOYER_ID)).thenThrow(EmployerAccessException.noCompany(EMPLOYER_ID));

        mockMvc.perform(get("/jobleaf/jobs/employer").accept(API).with(as("ROLE_EMPLOYER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value(
                        "You are not assigned to a company yet. Ask an admin to assign you to your company"));
    }

    @Test
    @DisplayName("Should create a job and map the enum display values to the domain")
    void create_returns201() throws Exception {
        when(createJobUseCase.createJob(eq(EMPLOYER_ID), any())).thenReturn(draft());

        mockMvc.perform(post("/jobleaf/jobs/employer").with(as("ROLE_EMPLOYER")).contentType(API).accept(API)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"));

        var details = ArgumentCaptor.forClass(JobDetails.class);
        verify(createJobUseCase).createJob(eq(EMPLOYER_ID), details.capture());
        assertThat(details.getValue().jobType()).isEqualTo("Full-time");
        assertThat(details.getValue().experienceLevel()).isEqualTo("Mid Level");
        assertThat(details.getValue().urgent()).isTrue();
    }

    @Test
    @DisplayName("Should reject missing fields and unknown enum values with 400 before the use case")
    void create_invalid_400() throws Exception {
        mockMvc.perform(post("/jobleaf/jobs/employer").with(as("ROLE_EMPLOYER")).contentType(API).accept(API)
                        .content("{\"title\":\"Frontend Developer\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/jobleaf/jobs/employer").with(as("ROLE_EMPLOYER")).contentType(API).accept(API)
                        .content(VALID_BODY.replace("\"Hybrid\"", "\"Sometimes\"")))
                .andExpect(status().isBadRequest());
        verify(createJobUseCase, never()).createJob(any(), any());
    }

    @Test
    @DisplayName("Should change a status, answer 404 for a foreign job and 409 for back-to-draft")
    void updateStatus() throws Exception {
        String url = "/jobleaf/jobs/{jobId}/status/employer";
        var job = draft();
        when(updateJobStatusUseCase.updateStatus(EMPLOYER_ID, job.id(), JobStatus.ACTIVE))
                .thenReturn(job.withStatus(JobStatus.ACTIVE, Instant.now()));
        mockMvc.perform(patch(url, job.id()).with(as("ROLE_EMPLOYER")).contentType(API).accept(API)
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        UUID foreign = UUID.randomUUID();
        when(updateJobStatusUseCase.updateStatus(EMPLOYER_ID, foreign, JobStatus.CLOSED))
                .thenThrow(new JobNotFoundException(foreign));
        mockMvc.perform(patch(url, foreign).with(as("ROLE_EMPLOYER")).contentType(API).accept(API)
                        .content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isNotFound());

        when(updateJobStatusUseCase.updateStatus(EMPLOYER_ID, job.id(), JobStatus.DRAFT))
                .thenThrow(new InvalidJobStatusChangeException(JobStatus.ACTIVE, JobStatus.DRAFT));
        mockMvc.perform(patch(url, job.id()).with(as("ROLE_EMPLOYER")).contentType(API).accept(API)
                        .content("{\"status\":\"DRAFT\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(patch(url, job.id()).with(as("ROLE_EMPLOYER")).contentType(API).accept(API)
                        .content("{\"status\":\"PAUSED\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should reject the status change without the CSRF token")
    void updateStatus_withoutCsrf_403() throws Exception {
        mockMvc.perform(patch("/jobleaf/jobs/{jobId}/status/employer", UUID.randomUUID()).accept(API)
                        .header("Authorization", bearer("ROLE_EMPLOYER")).contentType(API)
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isForbidden());
        verify(updateJobStatusUseCase, never()).updateStatus(any(), any(), any());
    }
}

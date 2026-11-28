package com.seby.jobleaf.infrastructure.web.controller;

import com.seby.jobleaf.domain.exceptions.jobapplication.JobApplicationConflictException;
import com.seby.jobleaf.domain.exceptions.profile.ProfileIncompleteException;
import com.seby.jobleaf.domain.model.job.Job;
import com.seby.jobleaf.domain.model.job.JobDetails;
import com.seby.jobleaf.domain.model.job.JobStatus;
import com.seby.jobleaf.domain.model.jobapplication.ApplicationStatus;
import com.seby.jobleaf.domain.model.jobapplication.JobApplication;
import com.seby.jobleaf.domain.port.in.jobapplication.ApplyForJobUseCase;
import com.seby.jobleaf.domain.port.in.jobapplication.ListMyApplicationsUseCase;
import com.seby.jobleaf.domain.port.in.jobapplication.WithdrawApplicationUseCase;
import com.seby.jobleaf.infrastructure.web.mapper.JobApplicationWebMapper;
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
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The job application endpoints through the real security chain, validation and mappers.
 */
@WebMvcTest(JobApplicationController.class)
@Import({JobLeafSecurityConfig.class, PathsConfig.class, ProblemDetailSecurityHandler.class, AccessTokenCookies.class,
        JobApplicationWebMapper.class, JobWebMapper.class})
class JobApplicationWebTest {

    private static final String API = "application/vnd.jobleaf+json;v=1.0";
    private static final UUID USER_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecretKey jwtSigningKey;

    @MockitoBean
    private JobLeafEmailPwdAuthenticationProvider authenticationProvider;

    @MockitoBean
    private ApplyForJobUseCase applyForJobUseCase;

    @MockitoBean
    private WithdrawApplicationUseCase withdrawApplicationUseCase;

    @MockitoBean
    private ListMyApplicationsUseCase listMyApplicationsUseCase;

    private String bearer(String role) {
        return "Bearer " + Jwts.builder().issuer("jobleaf").subject(USER_ID.toString())
                .claim("email", "jane@example.com").claim("roles", role)
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

    private static JobApplication application(ApplicationStatus status) {
        var details = new JobDetails("Frontend Developer", "Bucharest", "Hybrid", "Full-time", "Technology",
                "Mid Level", BigDecimal.ONE, BigDecimal.TEN, "EUR", "year", "We are looking for a developer.",
                List.of("React"), List.of("Remote Work"), Instant.now().plus(Duration.ofDays(30)), false, false, false);
        Job job = Job.create(UUID.randomUUID(), UUID.randomUUID(), details, Instant.now(), UUID.randomUUID())
                .withStatus(JobStatus.ACTIVE, Instant.now());
        var created = JobApplication.create(UUID.randomUUID(), USER_ID, job, "Hello", Instant.now());
        return new JobApplication(created.id(), USER_ID, job, created.appliedAt(), status, "Hello",
                "secret employer note", null, USER_ID, null, USER_ID);
    }

    @Test
    @DisplayName("Should only let job seekers in")
    void otherRoles_rejected() throws Exception {
        mockMvc.perform(get("/jobleaf/job-applications/jobseeker").accept(API)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/jobleaf/job-applications/jobseeker").accept(API).with(as("ROLE_EMPLOYER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should apply and answer 201 with the job, but never with the employer's notes")
    void apply_201() throws Exception {
        var application = application(ApplicationStatus.PENDING);
        when(applyForJobUseCase.applyForJob(USER_ID, application.job().id(), "Hello")).thenReturn(application);

        mockMvc.perform(post("/jobleaf/job-applications/jobseeker").with(as("ROLE_JOB_SEEKER")).contentType(API)
                        .accept(API).content("{\"jobId\":\"" + application.job().id() + "\",\"coverLetter\":\"Hello\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.job.title").value("Frontend Developer"))
                .andExpect(jsonPath("$.notes").doesNotExist());
    }

    @Test
    @DisplayName("Should accept applying without a cover letter, and reject a body without jobId")
    void apply_validation() throws Exception {
        var application = application(ApplicationStatus.PENDING);
        when(applyForJobUseCase.applyForJob(eq(USER_ID), eq(application.job().id()), isNull())).thenReturn(application);
        mockMvc.perform(post("/jobleaf/job-applications/jobseeker").with(as("ROLE_JOB_SEEKER")).contentType(API)
                        .accept(API).content("{\"jobId\":\"" + application.job().id() + "\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/jobleaf/job-applications/jobseeker").with(as("ROLE_JOB_SEEKER")).contentType(API)
                        .accept(API).content("{\"coverLetter\":\"Hello\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should answer 409 with errors.profile when the profile has no resume, and 409 for applying twice")
    void apply_conflicts() throws Exception {
        UUID jobId = UUID.randomUUID();
        when(applyForJobUseCase.applyForJob(USER_ID, jobId, null)).thenThrow(new ProfileIncompleteException());
        mockMvc.perform(post("/jobleaf/job-applications/jobseeker").with(as("ROLE_JOB_SEEKER")).contentType(API)
                        .accept(API).content("{\"jobId\":\"" + jobId + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors.profile").exists());

        UUID other = UUID.randomUUID();
        when(applyForJobUseCase.applyForJob(USER_ID, other, null))
                .thenThrow(JobApplicationConflictException.alreadyApplied(other));
        mockMvc.perform(post("/jobleaf/job-applications/jobseeker").with(as("ROLE_JOB_SEEKER")).contentType(API)
                        .accept(API).content("{\"jobId\":\"" + other + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    @DisplayName("Should withdraw and list")
    void withdrawAndList() throws Exception {
        var withdrawn = application(ApplicationStatus.WITHDRAWN);
        when(withdrawApplicationUseCase.withdrawApplication(USER_ID, withdrawn.job().id())).thenReturn(withdrawn);
        mockMvc.perform(delete("/jobleaf/job-applications/{jobId}/jobseeker", withdrawn.job().id()).accept(API)
                        .with(as("ROLE_JOB_SEEKER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WITHDRAWN"));

        when(listMyApplicationsUseCase.listMyApplications(USER_ID)).thenReturn(List.of(withdrawn));
        mockMvc.perform(get("/jobleaf/job-applications/jobseeker").accept(API).with(as("ROLE_JOB_SEEKER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].job.id").value(withdrawn.job().id().toString()));
    }

    @Test
    @DisplayName("Should reject applying without the CSRF token")
    void apply_withoutCsrf_403() throws Exception {
        mockMvc.perform(post("/jobleaf/job-applications/jobseeker").header("Authorization", bearer("ROLE_JOB_SEEKER"))
                        .contentType(API).accept(API).content("{\"jobId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isForbidden());
        verify(applyForJobUseCase, never()).applyForJob(any(), any(), any());
    }
}

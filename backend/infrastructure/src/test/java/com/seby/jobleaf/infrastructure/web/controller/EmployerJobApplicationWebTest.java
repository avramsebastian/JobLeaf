package com.seby.jobleaf.infrastructure.web.controller;

import com.seby.jobleaf.domain.exceptions.job.JobNotFoundException;
import com.seby.jobleaf.domain.exceptions.jobapplication.JobApplicationConflictException;
import com.seby.jobleaf.domain.model.job.Job;
import com.seby.jobleaf.domain.model.job.JobDetails;
import com.seby.jobleaf.domain.model.job.JobStatus;
import com.seby.jobleaf.domain.model.jobapplication.Applicant;
import com.seby.jobleaf.domain.model.jobapplication.ApplicationStatus;
import com.seby.jobleaf.domain.model.jobapplication.JobApplication;
import com.seby.jobleaf.domain.model.jobapplication.ReceivedApplication;
import com.seby.jobleaf.domain.model.profile.ProfileDetails;
import com.seby.jobleaf.domain.port.in.jobapplication.ListReceivedApplicationsUseCase;
import com.seby.jobleaf.domain.port.in.jobapplication.UpdateJobApplicationUseCase;
import com.seby.jobleaf.domain.port.in.jobapplication.ReadApplicantFilesUseCase;
import com.seby.jobleaf.domain.exceptions.profile.ProfileNotFoundException;
import com.seby.jobleaf.domain.model.profile.ProfileFile;
import com.seby.jobleaf.domain.model.profile.ProfileFileType;
import com.seby.jobleaf.infrastructure.web.mapper.ProfileWebMapper;
import com.seby.jobleaf.infrastructure.web.mapper.ReceivedApplicationWebMapper;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The employer's job application endpoints through the real security chain, validation and mapper.
 */
@WebMvcTest(EmployerJobApplicationController.class)
@Import({JobLeafSecurityConfig.class, PathsConfig.class, ProblemDetailSecurityHandler.class, AccessTokenCookies.class,
        ReceivedApplicationWebMapper.class, ProfileWebMapper.class})
class EmployerJobApplicationWebTest {

    private static final String API = "application/vnd.jobleaf+json;v=1.0";
    private static final UUID EMPLOYER_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecretKey jwtSigningKey;

    @MockitoBean
    private JobLeafEmailPwdAuthenticationProvider authenticationProvider;

    @MockitoBean
    private ListReceivedApplicationsUseCase listReceivedApplicationsUseCase;

    @MockitoBean
    private UpdateJobApplicationUseCase updateJobApplicationUseCase;

    @MockitoBean
    private ReadApplicantFilesUseCase readApplicantFilesUseCase;

    private String bearer(String role) {
        return "Bearer " + Jwts.builder().issuer("jobleaf").subject(EMPLOYER_ID.toString())
                .claim("email", "boss@example.com").claim("roles", role)
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

    private static ReceivedApplication received(ApplicationStatus status, String notes) {
        var details = new JobDetails("Frontend Developer", "Bucharest", "Hybrid", "Full-time", "Technology",
                "Mid Level", BigDecimal.ONE, BigDecimal.TEN, "EUR", "year", "We are looking for a developer.",
                List.of("React"), List.of("Remote Work"), Instant.now().plus(Duration.ofDays(30)), false, false, false);
        Job job = Job.create(UUID.randomUUID(), UUID.randomUUID(), details, Instant.now(), UUID.randomUUID())
                .withStatus(JobStatus.ACTIVE, Instant.now());
        UUID seekerId = UUID.randomUUID();
        var created = JobApplication.create(UUID.randomUUID(), seekerId, job, "Hello", Instant.now());
        var application = new JobApplication(created.id(), seekerId, job, created.appliedAt(), status, "Hello", notes,
                null, seekerId, null, EMPLOYER_ID);
        var profile = new ProfileDetails("Frontend Developer", "Bucharest", "Mid Level", "React developer.", null);
        return new ReceivedApplication(application,
                new Applicant(seekerId, "Jane", "jane@example.com", "0700000000", profile, true, false));
    }

    @Test
    @DisplayName("Should only let employers in")
    void otherRoles_rejected() throws Exception {
        UUID jobId = UUID.randomUUID();
        mockMvc.perform(get("/jobleaf/jobs/{jobId}/applications/employer", jobId).accept(API))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/jobleaf/jobs/{jobId}/applications/employer", jobId).accept(API)
                        .with(as("ROLE_JOB_SEEKER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should list the applications with the applicant and the employer's notes, but not the whole job")
    void list_200() throws Exception {
        var received = received(ApplicationStatus.PENDING, "call back");
        UUID jobId = received.application().job().id();
        when(listReceivedApplicationsUseCase.listReceivedApplications(EMPLOYER_ID, jobId)).thenReturn(List.of(received));

        mockMvc.perform(get("/jobleaf/jobs/{jobId}/applications/employer", jobId).accept(API).with(as("ROLE_EMPLOYER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].jobId").value(jobId.toString()))
                .andExpect(jsonPath("$[0].applicant.name").value("Jane"))
                .andExpect(jsonPath("$[0].applicant.jobTitle").value("Frontend Developer"))
                .andExpect(jsonPath("$[0].applicant.hasResume").value(true))
                .andExpect(jsonPath("$[0].notes").value("call back"))
                .andExpect(jsonPath("$[0].job").doesNotExist());
    }

    @Test
    @DisplayName("Should answer 404 for a job of another company")
    void list_404() throws Exception {
        UUID jobId = UUID.randomUUID();
        when(listReceivedApplicationsUseCase.listReceivedApplications(EMPLOYER_ID, jobId))
                .thenThrow(new JobNotFoundException(jobId));

        mockMvc.perform(get("/jobleaf/jobs/{jobId}/applications/employer", jobId).accept(API).with(as("ROLE_EMPLOYER")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should update status and notes, and accept a body without notes")
    void update_200() throws Exception {
        var received = received(ApplicationStatus.SHORTLISTED, "Strong CV");
        UUID id = received.application().id();
        when(updateJobApplicationUseCase.updateJobApplication(EMPLOYER_ID, id, ApplicationStatus.SHORTLISTED,
                "Strong CV")).thenReturn(received);
        mockMvc.perform(patch("/jobleaf/jobs/applications/{applicationId}/employer", id).with(as("ROLE_EMPLOYER"))
                        .contentType(API).accept(API).content("{\"status\":\"SHORTLISTED\",\"notes\":\"Strong CV\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHORTLISTED"))
                .andExpect(jsonPath("$.notes").value("Strong CV"));

        when(updateJobApplicationUseCase.updateJobApplication(any(), any(), any(), isNull())).thenReturn(received);
        mockMvc.perform(patch("/jobleaf/jobs/applications/{applicationId}/employer", id).with(as("ROLE_EMPLOYER"))
                        .contentType(API).accept(API).content("{\"status\":\"SHORTLISTED\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should answer 400 without status or with an unknown one, and 409 for a withdrawn application")
    void update_errors() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(patch("/jobleaf/jobs/applications/{applicationId}/employer", id).with(as("ROLE_EMPLOYER"))
                        .contentType(API).accept(API).content("{\"notes\":\"hi\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/jobleaf/jobs/applications/{applicationId}/employer", id).with(as("ROLE_EMPLOYER"))
                        .contentType(API).accept(API).content("{\"status\":\"HIRED\"}"))
                .andExpect(status().isBadRequest());

        when(updateJobApplicationUseCase.updateJobApplication(EMPLOYER_ID, id, ApplicationStatus.REVIEWED, null))
                .thenThrow(JobApplicationConflictException.withdrawn(id));
        mockMvc.perform(patch("/jobleaf/jobs/applications/{applicationId}/employer", id).with(as("ROLE_EMPLOYER"))
                        .contentType(API).accept(API).content("{\"status\":\"REVIEWED\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Should send the applicant's resume as a PDF download and the picture inline, only to employers")
    void files() throws Exception {
        UUID id = UUID.randomUUID();
        byte[] pdf = "%PDF-1.7".getBytes();
        when(readApplicantFilesUseCase.getApplicantResume(EMPLOYER_ID, id))
                .thenReturn(new ProfileFile("cv.pdf", ProfileFileType.PDF, pdf));
        mockMvc.perform(get("/jobleaf/jobs/applications/{applicationId}/resume/employer", id).accept("application/pdf")
                        .with(as("ROLE_EMPLOYER")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", startsWith("attachment")))
                .andExpect(content().bytes(pdf));

        when(readApplicantFilesUseCase.getApplicantProfilePicture(EMPLOYER_ID, id))
                .thenThrow(ProfileNotFoundException.noFile("profile picture"));
        mockMvc.perform(get("/jobleaf/jobs/applications/{applicationId}/picture/employer", id).accept("image/jpeg")
                        .with(as("ROLE_EMPLOYER")))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/jobleaf/jobs/applications/{applicationId}/resume/employer", id).accept("application/pdf")
                        .with(as("ROLE_JOB_SEEKER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should reject updating without the CSRF token")
    void update_withoutCsrf_403() throws Exception {
        mockMvc.perform(patch("/jobleaf/jobs/applications/{applicationId}/employer", UUID.randomUUID())
                        .header("Authorization", bearer("ROLE_EMPLOYER"))
                        .contentType(API).accept(API).content("{\"status\":\"REVIEWED\"}"))
                .andExpect(status().isForbidden());
        verify(updateJobApplicationUseCase, never()).updateJobApplication(any(), any(), any(), any());
    }
}

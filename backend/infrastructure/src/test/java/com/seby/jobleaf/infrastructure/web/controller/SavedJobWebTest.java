package com.seby.jobleaf.infrastructure.web.controller;

import com.seby.jobleaf.domain.exceptions.job.JobNotFoundException;
import com.seby.jobleaf.domain.model.job.Job;
import com.seby.jobleaf.domain.model.job.JobDetails;
import com.seby.jobleaf.domain.model.job.JobStatus;
import com.seby.jobleaf.domain.port.in.savedjob.ListSavedJobsUseCase;
import com.seby.jobleaf.domain.port.in.savedjob.SaveJobUseCase;
import com.seby.jobleaf.domain.port.in.savedjob.UnsaveJobUseCase;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The saved job endpoints through the real security chain and mapper.
 */
@WebMvcTest(SavedJobController.class)
@Import({JobLeafSecurityConfig.class, PathsConfig.class, ProblemDetailSecurityHandler.class, AccessTokenCookies.class,
        JobWebMapper.class})
class SavedJobWebTest {

    private static final String API = "application/vnd.jobleaf+json;v=1.0";
    private static final UUID USER_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecretKey jwtSigningKey;

    @MockitoBean
    private JobLeafEmailPwdAuthenticationProvider authenticationProvider;

    @MockitoBean
    private SaveJobUseCase saveJobUseCase;

    @MockitoBean
    private UnsaveJobUseCase unsaveJobUseCase;

    @MockitoBean
    private ListSavedJobsUseCase listSavedJobsUseCase;

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

    private static Job activeJob() {
        var details = new JobDetails("Frontend Developer", "Bucharest", "Hybrid", "Full-time", "Technology",
                "Mid Level", BigDecimal.ONE, BigDecimal.TEN, "EUR", "year", "We are looking for a developer.",
                List.of("React"), List.of("Remote Work"), Instant.now().plus(Duration.ofDays(30)), false, false, false);
        return Job.create(UUID.randomUUID(), UUID.randomUUID(), details, Instant.now(), UUID.randomUUID())
                .withStatus(JobStatus.ACTIVE, Instant.now());
    }

    @Test
    @DisplayName("Should only let job seekers in")
    void otherRoles_rejected() throws Exception {
        mockMvc.perform(get("/jobleaf/saved-jobs/jobseeker").accept(API)).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/jobleaf/saved-jobs/{jobId}/jobseeker", UUID.randomUUID()).accept(API)
                        .with(as("ROLE_EMPLOYER")))
                .andExpect(status().isForbidden());
        verify(saveJobUseCase, never()).saveJob(any(), any());
    }

    @Test
    @DisplayName("Should list the saved jobs of the logged-in job seeker")
    void list() throws Exception {
        var job = activeJob();
        when(listSavedJobsUseCase.listSavedJobs(USER_ID)).thenReturn(List.of(job));

        mockMvc.perform(get("/jobleaf/saved-jobs/jobseeker").accept(API).with(as("ROLE_JOB_SEEKER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(job.id().toString()))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    @DisplayName("Should save a job and answer with it, or 404 for a job that can not be saved")
    void save() throws Exception {
        var job = activeJob();
        when(saveJobUseCase.saveJob(USER_ID, job.id())).thenReturn(job);
        mockMvc.perform(post("/jobleaf/saved-jobs/{jobId}/jobseeker", job.id()).accept(API).with(as("ROLE_JOB_SEEKER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Frontend Developer"));

        UUID draft = UUID.randomUUID();
        when(saveJobUseCase.saveJob(USER_ID, draft)).thenThrow(new JobNotFoundException(draft));
        mockMvc.perform(post("/jobleaf/saved-jobs/{jobId}/jobseeker", draft).accept(API).with(as("ROLE_JOB_SEEKER")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should remove a saved job and answer with a message")
    void unsave() throws Exception {
        UUID jobId = UUID.randomUUID();

        mockMvc.perform(delete("/jobleaf/saved-jobs/{jobId}/jobseeker", jobId).accept(API).with(as("ROLE_JOB_SEEKER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Job removed from saved jobs"));
        verify(unsaveJobUseCase).unsaveJob(USER_ID, jobId);
    }

    @Test
    @DisplayName("Should reject saving without the CSRF token")
    void save_withoutCsrf_403() throws Exception {
        mockMvc.perform(post("/jobleaf/saved-jobs/{jobId}/jobseeker", UUID.randomUUID()).accept(API)
                        .header("Authorization", bearer("ROLE_JOB_SEEKER")))
                .andExpect(status().isForbidden());
        verify(saveJobUseCase, never()).saveJob(any(), any());
    }
}

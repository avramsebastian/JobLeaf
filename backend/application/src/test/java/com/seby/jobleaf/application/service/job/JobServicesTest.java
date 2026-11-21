package com.seby.jobleaf.application.service.job;

import com.seby.jobleaf.domain.exceptions.job.InvalidJobStatusChangeException;
import com.seby.jobleaf.domain.exceptions.job.JobNotFoundException;
import com.seby.jobleaf.domain.exceptions.user.EmployerAccessException;
import com.seby.jobleaf.domain.exceptions.user.UserNotFoundException;
import com.seby.jobleaf.domain.model.job.Job;
import com.seby.jobleaf.domain.model.job.JobDetails;
import com.seby.jobleaf.domain.model.user.JobLeafUser;
import com.seby.jobleaf.domain.model.job.JobStatus;
import com.seby.jobleaf.domain.model.user.RoleName;
import com.seby.jobleaf.domain.port.out.auth.UserRepositoryPort;
import com.seby.jobleaf.domain.port.out.job.JobRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The checks every job service makes before touching jobs: the user exists, is (still) an employer with a company,
 * and only works on jobs of that company.
 */
class JobServicesTest {

    static final UUID EMPLOYER_ID = UUID.randomUUID();
    static final UUID COMPANY_ID = UUID.randomUUID();
    static final JobLeafUser EMPLOYER = new JobLeafUser(EMPLOYER_ID, "Emma", "emma@example.com", null,
            RoleName.ROLE_EMPLOYER, COMPANY_ID, "DerpCorp", null);
    static final JobDetails DETAILS = new JobDetails("Frontend Developer", "Bucharest", "Hybrid", "Full-time",
            "Technology", "Mid Level", BigDecimal.valueOf(50_000), BigDecimal.valueOf(80_000), "EUR", "year",
            "We are looking for a frontend developer.", List.of("React"), List.of("Remote Work"),
            Instant.now().plus(Duration.ofDays(30)), false, false, true);

    static Job job(UUID companyId, JobStatus status) {
        var draft = Job.create(UUID.randomUUID(), companyId, DETAILS, Instant.parse("2026-11-01T10:00:00Z"),
                EMPLOYER_ID);
        return status == JobStatus.DRAFT ? draft : draft.withStatus(status, Instant.parse("2026-11-02T10:00:00Z"));
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class ListJobs {

        @Mock
        UserRepositoryPort userRepositoryPort;
        @Mock
        JobRepositoryPort jobRepositoryPort;
        @InjectMocks
        ListEmployerJobsService service;

        @Test
        @DisplayName("Should list the jobs of the employer's own company")
        void listsOwnCompanyJobs() {
            var jobs = List.of(job(COMPANY_ID, JobStatus.DRAFT));
            when(userRepositoryPort.findById(EMPLOYER_ID)).thenReturn(Optional.of(EMPLOYER));
            when(jobRepositoryPort.findAllByCompanyId(COMPANY_ID)).thenReturn(jobs);

            assertThat(service.listJobs(EMPLOYER_ID)).isEqualTo(jobs);
        }

        @Test
        @DisplayName("Should refuse an employer without company and an unknown user")
        void refusesWithoutCompanyOrUser() {
            var noCompany = new JobLeafUser(EMPLOYER_ID, "Emma", "emma@example.com", null, RoleName.ROLE_EMPLOYER,
                    null, null, null);
            when(userRepositoryPort.findById(EMPLOYER_ID)).thenReturn(Optional.of(noCompany));
            assertThatThrownBy(() -> service.listJobs(EMPLOYER_ID)).isInstanceOf(EmployerAccessException.class);

            UUID unknown = UUID.randomUUID();
            when(userRepositoryPort.findById(unknown)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.listJobs(unknown)).isInstanceOf(UserNotFoundException.class);
            verifyNoInteractions(jobRepositoryPort);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class CreateJob {

        @Mock
        UserRepositoryPort userRepositoryPort;
        @Mock
        JobRepositoryPort jobRepositoryPort;
        @InjectMocks
        CreateJobService service;

        @Test
        @DisplayName("Should create a DRAFT job for the employer's company")
        void createsDraftForOwnCompany() {
            when(userRepositoryPort.findById(EMPLOYER_ID)).thenReturn(Optional.of(EMPLOYER));
            when(jobRepositoryPort.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

            Job created = service.createJob(EMPLOYER_ID, DETAILS);

            var stored = ArgumentCaptor.forClass(Job.class);
            verify(jobRepositoryPort).create(stored.capture());
            assertThat(stored.getValue().companyId()).isEqualTo(COMPANY_ID);
            assertThat(created.currentStatus()).isEqualTo(JobStatus.DRAFT);
        }

        @Test
        @DisplayName("Should refuse a user who is no longer an employer (stale token)")
        void refusesNonEmployer() {
            var seeker = new JobLeafUser(EMPLOYER_ID, "Emma", "emma@example.com", null, RoleName.ROLE_JOB_SEEKER,
                    null, null, null);
            when(userRepositoryPort.findById(EMPLOYER_ID)).thenReturn(Optional.of(seeker));

            assertThatThrownBy(() -> service.createJob(EMPLOYER_ID, DETAILS))
                    .isInstanceOf(EmployerAccessException.class);
            verify(jobRepositoryPort, never()).create(any());
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class UpdateStatus {

        @Mock
        UserRepositoryPort userRepositoryPort;
        @Mock
        JobRepositoryPort jobRepositoryPort;
        @InjectMocks
        UpdateJobStatusService service;

        @Test
        @DisplayName("Should publish a draft of the employer's company")
        void publishesOwnDraft() {
            var draft = job(COMPANY_ID, JobStatus.DRAFT);
            when(userRepositoryPort.findById(EMPLOYER_ID)).thenReturn(Optional.of(EMPLOYER));
            when(jobRepositoryPort.findById(draft.id())).thenReturn(Optional.of(draft));
            when(jobRepositoryPort.updateStatus(any())).thenAnswer(invocation -> invocation.getArgument(0));

            Job updated = service.updateStatus(EMPLOYER_ID, draft.id(), JobStatus.ACTIVE);

            assertThat(updated.currentStatus()).isEqualTo(JobStatus.ACTIVE);
            assertThat(updated.postedDate()).isAfter(draft.postedDate());
        }

        @Test
        @DisplayName("Should answer a job of another company like a missing one (404)")
        void otherCompanyJob_notFound() {
            var foreign = job(UUID.randomUUID(), JobStatus.DRAFT);
            when(userRepositoryPort.findById(EMPLOYER_ID)).thenReturn(Optional.of(EMPLOYER));
            when(jobRepositoryPort.findById(foreign.id())).thenReturn(Optional.of(foreign));

            assertThatThrownBy(() -> service.updateStatus(EMPLOYER_ID, foreign.id(), JobStatus.ACTIVE))
                    .isInstanceOf(JobNotFoundException.class);
            verify(jobRepositoryPort, never()).updateStatus(any());
        }

        @Test
        @DisplayName("Should refuse going back to DRAFT and save nothing for the same status")
        void transitionRules() {
            var active = job(COMPANY_ID, JobStatus.ACTIVE);
            when(userRepositoryPort.findById(EMPLOYER_ID)).thenReturn(Optional.of(EMPLOYER));
            when(jobRepositoryPort.findById(active.id())).thenReturn(Optional.of(active));

            assertThatThrownBy(() -> service.updateStatus(EMPLOYER_ID, active.id(), JobStatus.DRAFT))
                    .isInstanceOf(InvalidJobStatusChangeException.class);
            assertThat(service.updateStatus(EMPLOYER_ID, active.id(), JobStatus.ACTIVE)).isSameAs(active);
            verify(jobRepositoryPort, never()).updateStatus(any());
        }
    }
}

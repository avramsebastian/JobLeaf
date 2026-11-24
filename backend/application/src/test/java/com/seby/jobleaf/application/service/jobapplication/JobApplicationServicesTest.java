package com.seby.jobleaf.application.service.jobapplication;

import com.seby.jobleaf.domain.exceptions.job.JobNotFoundException;
import com.seby.jobleaf.domain.exceptions.jobapplication.JobApplicationConflictException;
import com.seby.jobleaf.domain.exceptions.jobapplication.JobApplicationNotFoundException;
import com.seby.jobleaf.domain.exceptions.profile.ProfileIncompleteException;
import com.seby.jobleaf.domain.exceptions.user.JobSeekerAccessException;
import com.seby.jobleaf.domain.model.job.Job;
import com.seby.jobleaf.domain.model.job.JobDetails;
import com.seby.jobleaf.domain.model.job.JobStatus;
import com.seby.jobleaf.domain.model.jobapplication.ApplicationStatus;
import com.seby.jobleaf.domain.model.jobapplication.JobApplication;
import com.seby.jobleaf.domain.model.profile.Profile;
import com.seby.jobleaf.domain.model.profile.ProfileDetails;
import com.seby.jobleaf.domain.model.profile.ProfileFile;
import com.seby.jobleaf.domain.model.profile.ProfileFileType;
import com.seby.jobleaf.domain.model.user.JobLeafUser;
import com.seby.jobleaf.domain.model.user.RoleName;
import com.seby.jobleaf.domain.port.out.auth.UserRepositoryPort;
import com.seby.jobleaf.domain.port.out.job.JobRepositoryPort;
import com.seby.jobleaf.domain.port.out.jobapplication.JobApplicationRepositoryPort;
import com.seby.jobleaf.domain.port.out.profile.ProfileRepositoryPort;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JobApplicationServicesTest {

    static final UUID USER_ID = UUID.randomUUID();
    static final JobLeafUser SEEKER = new JobLeafUser(USER_ID, "Jane", "jane@example.com", null,
            RoleName.ROLE_JOB_SEEKER, null, null, null);
    static final ProfileDetails DETAILS = new ProfileDetails("Frontend Developer", "Bucharest", "Mid Level",
            "Frontend developer with React experience.", null);
    static final Profile WITH_RESUME = Profile.create(UUID.randomUUID(), USER_ID, DETAILS)
            .withResume(ProfileFile.metadata("cv.pdf", ProfileFileType.PDF));
    static final Profile WITHOUT_RESUME = Profile.create(UUID.randomUUID(), USER_ID, DETAILS);

    static Job job(JobStatus status, Instant deadline) {
        var details = new JobDetails("Frontend Developer", "Bucharest", "Hybrid", "Full-time", "Technology",
                "Mid Level", BigDecimal.ONE, BigDecimal.TEN, "EUR", "year", "We are looking for a developer.",
                List.of("React"), List.of("Remote Work"), Instant.now().plus(Duration.ofDays(30)), false, false, false);
        var draft = Job.create(UUID.randomUUID(), UUID.randomUUID(), details, Instant.now(), UUID.randomUUID());
        var withStatus = status == JobStatus.DRAFT ? draft : draft.withStatus(status, Instant.now());
        // the deadline is set directly: JobDetails only accepts future deadlines, a passed one comes from stored data
        return new Job(withStatus.id(), withStatus.companyId(), withStatus.title(), withStatus.location(),
                withStatus.workType(), withStatus.jobType(), withStatus.category(), withStatus.experienceLevel(),
                withStatus.salaryMin(), withStatus.salaryMax(), withStatus.salaryCurrency(), withStatus.salaryPeriod(),
                withStatus.description(), withStatus.requirements(), withStatus.benefits(), withStatus.postedDate(),
                deadline, 0, false, false, false, withStatus.status(), null, null, null, null);
    }

    static Job openJob() {
        return job(JobStatus.ACTIVE, Instant.now().plus(Duration.ofDays(10)));
    }

    static JobApplication application(Job job, ApplicationStatus status) {
        var created = JobApplication.create(UUID.randomUUID(), USER_ID, job, null, Instant.now());
        return new JobApplication(created.id(), USER_ID, job, created.appliedAt(), status, null, null,
                null, USER_ID, null, USER_ID);
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class Apply {

        @Mock
        UserRepositoryPort userRepositoryPort;
        @Mock
        JobRepositoryPort jobRepositoryPort;
        @Mock
        ProfileRepositoryPort profileRepositoryPort;
        @Mock
        JobApplicationRepositoryPort jobApplicationRepositoryPort;
        @InjectMocks
        ApplyForJobService service;

        @Test
        @DisplayName("Should create a PENDING application and count it on the job")
        void applies() {
            var job = openJob();
            when(userRepositoryPort.findById(USER_ID)).thenReturn(Optional.of(SEEKER));
            when(jobRepositoryPort.findById(job.id())).thenReturn(Optional.of(job));
            when(profileRepositoryPort.findByUserId(USER_ID, false)).thenReturn(Optional.of(WITH_RESUME));
            when(jobApplicationRepositoryPort.findByUserAndJob(USER_ID, job.id())).thenReturn(Optional.empty());
            when(jobApplicationRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            var application = service.applyForJob(USER_ID, job.id(), "Hello");

            assertThat(application.status()).isEqualTo(ApplicationStatus.PENDING);
            assertThat(application.coverLetter()).isEqualTo("Hello");
            verify(jobRepositoryPort).changeApplicationsCount(job.id(), 1);
        }

        @Test
        @DisplayName("Should refuse a draft (404) and a job whose deadline passed (409)")
        void jobMustBeOpen() {
            var draft = job(JobStatus.DRAFT, null);
            var expired = job(JobStatus.ACTIVE, Instant.now().minus(Duration.ofDays(1)));
            when(userRepositoryPort.findById(USER_ID)).thenReturn(Optional.of(SEEKER));
            when(jobRepositoryPort.findById(draft.id())).thenReturn(Optional.of(draft));
            when(jobRepositoryPort.findById(expired.id())).thenReturn(Optional.of(expired));

            assertThatThrownBy(() -> service.applyForJob(USER_ID, draft.id(), null))
                    .isInstanceOf(JobNotFoundException.class);
            assertThatThrownBy(() -> service.applyForJob(USER_ID, expired.id(), null))
                    .isInstanceOf(JobApplicationConflictException.class)
                    .hasMessageContaining("deadline");
            verify(jobApplicationRepositoryPort, never()).save(any());
        }

        @Test
        @DisplayName("Should require a profile with a resume")
        void profileWithResumeRequired() {
            var job = openJob();
            when(userRepositoryPort.findById(USER_ID)).thenReturn(Optional.of(SEEKER));
            when(jobRepositoryPort.findById(job.id())).thenReturn(Optional.of(job));

            when(profileRepositoryPort.findByUserId(USER_ID, false)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.applyForJob(USER_ID, job.id(), null))
                    .isInstanceOf(ProfileIncompleteException.class);

            when(profileRepositoryPort.findByUserId(USER_ID, false)).thenReturn(Optional.of(WITHOUT_RESUME));
            assertThatThrownBy(() -> service.applyForJob(USER_ID, job.id(), null))
                    .isInstanceOf(ProfileIncompleteException.class);
            verify(jobApplicationRepositoryPort, never()).save(any());
        }

        @Test
        @DisplayName("Should refuse applying twice")
        void alreadyApplied_conflict() {
            var job = openJob();
            when(userRepositoryPort.findById(USER_ID)).thenReturn(Optional.of(SEEKER));
            when(jobRepositoryPort.findById(job.id())).thenReturn(Optional.of(job));
            when(profileRepositoryPort.findByUserId(USER_ID, false)).thenReturn(Optional.of(WITH_RESUME));
            when(jobApplicationRepositoryPort.findByUserAndJob(USER_ID, job.id()))
                    .thenReturn(Optional.of(application(job, ApplicationStatus.REVIEWED)));

            assertThatThrownBy(() -> service.applyForJob(USER_ID, job.id(), null))
                    .isInstanceOf(JobApplicationConflictException.class)
                    .hasMessageContaining("already applied");
            verify(jobRepositoryPort, never()).changeApplicationsCount(any(), anyInt());
        }

        @Test
        @DisplayName("Should reopen a withdrawn application instead of creating a second one")
        void withdrawn_reopened() {
            var job = openJob();
            var withdrawn = application(job, ApplicationStatus.WITHDRAWN);
            when(userRepositoryPort.findById(USER_ID)).thenReturn(Optional.of(SEEKER));
            when(jobRepositoryPort.findById(job.id())).thenReturn(Optional.of(job));
            when(profileRepositoryPort.findByUserId(USER_ID, false)).thenReturn(Optional.of(WITH_RESUME));
            when(jobApplicationRepositoryPort.findByUserAndJob(USER_ID, job.id())).thenReturn(Optional.of(withdrawn));
            when(jobApplicationRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            var reopened = service.applyForJob(USER_ID, job.id(), null);

            assertThat(reopened.id()).isEqualTo(withdrawn.id());
            assertThat(reopened.status()).isEqualTo(ApplicationStatus.PENDING);
            verify(jobRepositoryPort).changeApplicationsCount(job.id(), 1);
        }

        @Test
        @DisplayName("Should refuse users who are not job seekers")
        void otherRole_refused() {
            var employer = new JobLeafUser(USER_ID, "Emma", "emma@example.com", null, RoleName.ROLE_EMPLOYER,
                    UUID.randomUUID(), "DerpCorp", null);
            when(userRepositoryPort.findById(USER_ID)).thenReturn(Optional.of(employer));

            assertThatThrownBy(() -> service.applyForJob(USER_ID, UUID.randomUUID(), null))
                    .isInstanceOf(JobSeekerAccessException.class);
            verifyNoInteractions(jobRepositoryPort, profileRepositoryPort, jobApplicationRepositoryPort);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class Withdraw {

        @Mock
        UserRepositoryPort userRepositoryPort;
        @Mock
        JobRepositoryPort jobRepositoryPort;
        @Mock
        JobApplicationRepositoryPort jobApplicationRepositoryPort;
        @InjectMocks
        WithdrawApplicationService service;

        @Test
        @DisplayName("Should withdraw the application and take it off the job's count")
        void withdraws() {
            var job = openJob();
            when(userRepositoryPort.findById(USER_ID)).thenReturn(Optional.of(SEEKER));
            when(jobApplicationRepositoryPort.findByUserAndJob(USER_ID, job.id()))
                    .thenReturn(Optional.of(application(job, ApplicationStatus.PENDING)));
            when(jobApplicationRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            assertThat(service.withdrawApplication(USER_ID, job.id()).status()).isEqualTo(ApplicationStatus.WITHDRAWN);

            var saved = ArgumentCaptor.forClass(JobApplication.class);
            verify(jobApplicationRepositoryPort).save(saved.capture());
            assertThat(saved.getValue().status()).isEqualTo(ApplicationStatus.WITHDRAWN);
            verify(jobRepositoryPort).changeApplicationsCount(job.id(), -1);
        }

        @Test
        @DisplayName("Should change nothing when already withdrawn, and 404 without an application")
        void idempotentAndNotFound() {
            var job = openJob();
            when(userRepositoryPort.findById(USER_ID)).thenReturn(Optional.of(SEEKER));
            when(jobApplicationRepositoryPort.findByUserAndJob(USER_ID, job.id()))
                    .thenReturn(Optional.of(application(job, ApplicationStatus.WITHDRAWN)));
            service.withdrawApplication(USER_ID, job.id());
            verify(jobApplicationRepositoryPort, never()).save(any());
            verify(jobRepositoryPort, never()).changeApplicationsCount(any(), anyInt());

            UUID other = UUID.randomUUID();
            when(jobApplicationRepositoryPort.findByUserAndJob(USER_ID, other)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.withdrawApplication(USER_ID, other))
                    .isInstanceOf(JobApplicationNotFoundException.class);
        }

        @Test
        @DisplayName("Should refuse withdrawing a rejected application")
        void rejected_conflict() {
            var job = openJob();
            when(userRepositoryPort.findById(USER_ID)).thenReturn(Optional.of(SEEKER));
            when(jobApplicationRepositoryPort.findByUserAndJob(USER_ID, job.id()))
                    .thenReturn(Optional.of(application(job, ApplicationStatus.REJECTED)));

            assertThatThrownBy(() -> service.withdrawApplication(USER_ID, job.id()))
                    .isInstanceOf(JobApplicationConflictException.class);
            verify(jobApplicationRepositoryPort, never()).save(any());
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class ListMine {

        @Mock
        UserRepositoryPort userRepositoryPort;
        @Mock
        JobApplicationRepositoryPort jobApplicationRepositoryPort;
        @InjectMocks
        ListMyApplicationsService service;

        @Test
        @DisplayName("Should list the user's applications")
        void list() {
            var applications = List.of(application(openJob(), ApplicationStatus.PENDING));
            when(userRepositoryPort.findById(USER_ID)).thenReturn(Optional.of(SEEKER));
            when(jobApplicationRepositoryPort.findAllByUser(USER_ID)).thenReturn(applications);

            assertThat(service.listMyApplications(USER_ID)).isEqualTo(applications);
        }
    }
}

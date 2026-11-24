package com.seby.jobleaf.application.service.savedjob;

import com.seby.jobleaf.domain.exceptions.job.JobNotFoundException;
import com.seby.jobleaf.domain.exceptions.user.JobSeekerAccessException;
import com.seby.jobleaf.domain.model.job.Job;
import com.seby.jobleaf.domain.model.job.JobDetails;
import com.seby.jobleaf.domain.model.job.JobStatus;
import com.seby.jobleaf.domain.model.user.JobLeafUser;
import com.seby.jobleaf.domain.model.user.RoleName;
import com.seby.jobleaf.domain.port.out.auth.UserRepositoryPort;
import com.seby.jobleaf.domain.port.out.job.JobRepositoryPort;
import com.seby.jobleaf.domain.port.out.savedjob.SavedJobRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

class SavedJobServicesTest {

    static final UUID USER_ID = UUID.randomUUID();
    static final JobLeafUser SEEKER = new JobLeafUser(USER_ID, "Jane", "jane@example.com", null,
            RoleName.ROLE_JOB_SEEKER, null, null, null);
    static final JobLeafUser EMPLOYER = new JobLeafUser(USER_ID, "Emma", "emma@example.com", null,
            RoleName.ROLE_EMPLOYER, UUID.randomUUID(), "DerpCorp", null);

    static Job job(JobStatus status) {
        var details = new JobDetails("Frontend Developer", "Bucharest", "Hybrid", "Full-time", "Technology",
                "Mid Level", BigDecimal.ONE, BigDecimal.TEN, "EUR", "year", "We are looking for a developer.",
                List.of("React"), List.of("Remote Work"), Instant.now().plus(Duration.ofDays(30)), false, false, false);
        var draft = Job.create(UUID.randomUUID(), UUID.randomUUID(), details, Instant.now(), UUID.randomUUID());
        return status == JobStatus.DRAFT ? draft : draft.withStatus(status, Instant.now());
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class Save {

        @Mock
        UserRepositoryPort userRepositoryPort;
        @Mock
        JobRepositoryPort jobRepositoryPort;
        @Mock
        SavedJobRepositoryPort savedJobRepositoryPort;
        @InjectMocks
        SaveJobService service;

        @Test
        @DisplayName("Should save a published job")
        void savesActiveJob() {
            var job = job(JobStatus.ACTIVE);
            when(userRepositoryPort.findById(USER_ID)).thenReturn(Optional.of(SEEKER));
            when(jobRepositoryPort.findById(job.id())).thenReturn(Optional.of(job));
            when(savedJobRepositoryPort.isSaved(USER_ID, job.id())).thenReturn(false);

            assertThat(service.saveJob(USER_ID, job.id())).isSameAs(job);
            verify(savedJobRepositoryPort).save(USER_ID, job.id());
        }

        @Test
        @DisplayName("Should not save the same job twice")
        void alreadySaved_noWrite() {
            var job = job(JobStatus.ACTIVE);
            when(userRepositoryPort.findById(USER_ID)).thenReturn(Optional.of(SEEKER));
            when(jobRepositoryPort.findById(job.id())).thenReturn(Optional.of(job));
            when(savedJobRepositoryPort.isSaved(USER_ID, job.id())).thenReturn(true);

            service.saveJob(USER_ID, job.id());

            verify(savedJobRepositoryPort, never()).save(any(), any());
        }

        @Test
        @DisplayName("Should answer drafts and closed jobs like missing jobs (404)")
        void notVisibleJob_notFound() {
            var draft = job(JobStatus.DRAFT);
            var closed = job(JobStatus.CLOSED);
            when(userRepositoryPort.findById(USER_ID)).thenReturn(Optional.of(SEEKER));
            when(jobRepositoryPort.findById(draft.id())).thenReturn(Optional.of(draft));
            when(jobRepositoryPort.findById(closed.id())).thenReturn(Optional.of(closed));

            assertThatThrownBy(() -> service.saveJob(USER_ID, draft.id())).isInstanceOf(JobNotFoundException.class);
            assertThatThrownBy(() -> service.saveJob(USER_ID, closed.id())).isInstanceOf(JobNotFoundException.class);
            verify(savedJobRepositoryPort, never()).save(any(), any());
        }

        @Test
        @DisplayName("Should refuse users who are not job seekers")
        void otherRole_refused() {
            when(userRepositoryPort.findById(USER_ID)).thenReturn(Optional.of(EMPLOYER));

            assertThatThrownBy(() -> service.saveJob(USER_ID, UUID.randomUUID()))
                    .isInstanceOf(JobSeekerAccessException.class);
            verifyNoInteractions(jobRepositoryPort, savedJobRepositoryPort);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class Unsave {

        @Mock
        UserRepositoryPort userRepositoryPort;
        @Mock
        SavedJobRepositoryPort savedJobRepositoryPort;
        @InjectMocks
        UnsaveJobService service;

        @Test
        @DisplayName("Should remove a saved job, and do nothing for a job that is not saved")
        void unsave() {
            UUID saved = UUID.randomUUID();
            UUID notSaved = UUID.randomUUID();
            when(userRepositoryPort.findById(USER_ID)).thenReturn(Optional.of(SEEKER));
            when(savedJobRepositoryPort.isSaved(USER_ID, saved)).thenReturn(true);
            when(savedJobRepositoryPort.isSaved(USER_ID, notSaved)).thenReturn(false);

            service.unsaveJob(USER_ID, saved);
            service.unsaveJob(USER_ID, notSaved);

            verify(savedJobRepositoryPort).remove(USER_ID, saved);
            verify(savedJobRepositoryPort, never()).remove(USER_ID, notSaved);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class ListSaved {

        @Mock
        UserRepositoryPort userRepositoryPort;
        @Mock
        SavedJobRepositoryPort savedJobRepositoryPort;
        @InjectMocks
        ListSavedJobsService service;

        @Test
        @DisplayName("Should list the user's saved jobs")
        void list() {
            var jobs = List.of(job(JobStatus.ACTIVE), job(JobStatus.CLOSED));
            when(userRepositoryPort.findById(USER_ID)).thenReturn(Optional.of(SEEKER));
            when(savedJobRepositoryPort.findSavedJobs(USER_ID)).thenReturn(jobs);

            assertThat(service.listSavedJobs(USER_ID)).isEqualTo(jobs);
        }
    }
}

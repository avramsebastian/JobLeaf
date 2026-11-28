package com.seby.jobleaf.application.service.jobapplication;

import com.seby.jobleaf.domain.exceptions.job.JobNotFoundException;
import com.seby.jobleaf.domain.exceptions.jobapplication.JobApplicationConflictException;
import com.seby.jobleaf.domain.exceptions.profile.ProfileIncompleteException;
import com.seby.jobleaf.domain.exceptions.user.UserNotFoundException;
import com.seby.jobleaf.domain.model.job.Job;
import com.seby.jobleaf.domain.model.jobapplication.JobApplication;
import com.seby.jobleaf.domain.port.in.jobapplication.ApplyForJobUseCase;
import com.seby.jobleaf.domain.port.out.auth.UserRepositoryPort;
import com.seby.jobleaf.domain.port.out.job.JobRepositoryPort;
import com.seby.jobleaf.domain.port.out.jobapplication.JobApplicationRepositoryPort;
import com.seby.jobleaf.domain.port.out.profile.ProfileRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Application service implementing {@link ApplyForJobUseCase}.
 */
@Service
@RequiredArgsConstructor
public class ApplyForJobService implements ApplyForJobUseCase {

    private static final Logger log = LoggerFactory.getLogger(ApplyForJobService.class);

    private final UserRepositoryPort userRepositoryPort;
    private final JobRepositoryPort jobRepositoryPort;
    private final ProfileRepositoryPort profileRepositoryPort;
    private final JobApplicationRepositoryPort jobApplicationRepositoryPort;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public JobApplication applyForJob(UUID userId, UUID jobId, String coverLetter) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(jobId, "jobId");
        Instant now = Instant.now();

        // 1. Who: a job seeker, according to the database (the token can be an hour old)
        userRepositoryPort.findById(userId)
                .orElseThrow(() -> UserNotFoundException.byId(userId))
                .checkIsJobSeeker();

        // 2. Which job: published, and still open for applications
        Job job = jobRepositoryPort.findById(jobId)
                .filter(Job::isVisibleToJobSeekers)
                .orElseThrow(() -> new JobNotFoundException(jobId));
        if (!job.isDeadlineOpen(now)) {
            throw JobApplicationConflictException.deadlinePassed(jobId);
        }

        // 3. The employer needs a resume to look at (without loading the file itself)
        boolean hasResume = profileRepositoryPort.findByUserId(userId, false)
                .map(profile -> profile.resume() != null)
                .orElse(false);
        if (!hasResume) {
            throw new ProfileIncompleteException();
        }

        // 4. One application per user and job: a withdrawn one is reopened, an active one is a conflict
        var existing = jobApplicationRepositoryPort.findByUserAndJob(userId, jobId);
        if (existing.isPresent() && !existing.get().isWithdrawn()) {
            throw JobApplicationConflictException.alreadyApplied(jobId);
        }
        JobApplication application = existing
                .map(withdrawn -> withdrawn.reapply(coverLetter, now))
                .orElseGet(() -> JobApplication.create(UUID.randomUUID(), userId, job, coverLetter, now));

        JobApplication saved = jobApplicationRepositoryPort.save(application);
        jobRepositoryPort.changeApplicationsCount(jobId, +1);
        log.info("User {} {} job {} ({})", userId, existing.isPresent() ? "re-applied for" : "applied for", jobId,
                job.title());
        return saved;
    }
}

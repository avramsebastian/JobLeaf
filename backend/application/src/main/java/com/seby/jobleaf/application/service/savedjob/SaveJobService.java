package com.seby.jobleaf.application.service.savedjob;

import com.seby.jobleaf.domain.exceptions.job.JobNotFoundException;
import com.seby.jobleaf.domain.exceptions.user.UserNotFoundException;
import com.seby.jobleaf.domain.model.job.Job;
import com.seby.jobleaf.domain.port.in.savedjob.SaveJobUseCase;
import com.seby.jobleaf.domain.port.out.auth.UserRepositoryPort;
import com.seby.jobleaf.domain.port.out.job.JobRepositoryPort;
import com.seby.jobleaf.domain.port.out.savedjob.SavedJobRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

/**
 * Application service implementing {@link SaveJobUseCase}.
 */
@Service
@RequiredArgsConstructor
public class SaveJobService implements SaveJobUseCase {

    private static final Logger log = LoggerFactory.getLogger(SaveJobService.class);

    private final UserRepositoryPort userRepositoryPort;
    private final JobRepositoryPort jobRepositoryPort;
    private final SavedJobRepositoryPort savedJobRepositoryPort;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Job saveJob(UUID userId, UUID jobId) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(jobId, "jobId");
        userRepositoryPort.findById(userId)
                .orElseThrow(() -> UserNotFoundException.byId(userId))
                .checkIsJobSeeker();

        // A draft or closed job is answered like a missing one: job seekers can not see it, so they can not save it
        Job job = jobRepositoryPort.findById(jobId)
                .filter(Job::isVisibleToJobSeekers)
                .orElseThrow(() -> new JobNotFoundException(jobId));

        if (savedJobRepositoryPort.isSaved(userId, jobId)) {
            log.info("Job {} was already saved by user {}", jobId, userId);
            return job;
        }
        savedJobRepositoryPort.save(userId, jobId);
        log.info("User {} saved job {} ({})", userId, jobId, job.title());
        return job;
    }
}

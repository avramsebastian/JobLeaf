package com.seby.jobleaf.application.service.savedjob;

import com.seby.jobleaf.domain.exceptions.user.UserNotFoundException;
import com.seby.jobleaf.domain.port.in.savedjob.UnsaveJobUseCase;
import com.seby.jobleaf.domain.port.out.auth.UserRepositoryPort;
import com.seby.jobleaf.domain.port.out.savedjob.SavedJobRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

/**
 * Application service implementing {@link UnsaveJobUseCase}.
 */
@Service
@RequiredArgsConstructor
public class UnsaveJobService implements UnsaveJobUseCase {

    private static final Logger log = LoggerFactory.getLogger(UnsaveJobService.class);

    private final UserRepositoryPort userRepositoryPort;
    private final SavedJobRepositoryPort savedJobRepositoryPort;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void unsaveJob(UUID userId, UUID jobId) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(jobId, "jobId");
        userRepositoryPort.findById(userId)
                .orElseThrow(() -> UserNotFoundException.byId(userId))
                .checkIsJobSeeker();

        // Idempotent: removing a job that is not saved (anymore) is not an error, the result is the same
        if (!savedJobRepositoryPort.isSaved(userId, jobId)) {
            log.info("Job {} was not saved by user {}, nothing to remove", jobId, userId);
            return;
        }
        savedJobRepositoryPort.remove(userId, jobId);
        log.info("User {} removed job {} from the saved jobs", userId, jobId);
    }
}

package com.seby.jobleaf.application.service.savedjob;

import com.seby.jobleaf.domain.exceptions.user.UserNotFoundException;
import com.seby.jobleaf.domain.model.job.Job;
import com.seby.jobleaf.domain.port.in.savedjob.ListSavedJobsUseCase;
import com.seby.jobleaf.domain.port.out.auth.UserRepositoryPort;
import com.seby.jobleaf.domain.port.out.savedjob.SavedJobRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Application service implementing {@link ListSavedJobsUseCase}.
 */
@Service
@RequiredArgsConstructor
public class ListSavedJobsService implements ListSavedJobsUseCase {

    private static final Logger log = LoggerFactory.getLogger(ListSavedJobsService.class);

    private final UserRepositoryPort userRepositoryPort;
    private final SavedJobRepositoryPort savedJobRepositoryPort;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<Job> listSavedJobs(UUID userId) {
        Objects.requireNonNull(userId, "userId");
        userRepositoryPort.findById(userId)
                .orElseThrow(() -> UserNotFoundException.byId(userId))
                .checkIsJobSeeker();

        var jobs = savedJobRepositoryPort.findSavedJobs(userId);
        log.info("Listed {} saved jobs of user {}", jobs.size(), userId);
        return jobs;
    }
}

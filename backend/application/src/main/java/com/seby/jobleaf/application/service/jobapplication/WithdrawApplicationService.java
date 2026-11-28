package com.seby.jobleaf.application.service.jobapplication;

import com.seby.jobleaf.domain.exceptions.jobapplication.JobApplicationNotFoundException;
import com.seby.jobleaf.domain.exceptions.user.UserNotFoundException;
import com.seby.jobleaf.domain.model.jobapplication.JobApplication;
import com.seby.jobleaf.domain.port.in.jobapplication.WithdrawApplicationUseCase;
import com.seby.jobleaf.domain.port.out.auth.UserRepositoryPort;
import com.seby.jobleaf.domain.port.out.job.JobRepositoryPort;
import com.seby.jobleaf.domain.port.out.jobapplication.JobApplicationRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

/**
 * Application service implementing {@link WithdrawApplicationUseCase}.
 */
@Service
@RequiredArgsConstructor
public class WithdrawApplicationService implements WithdrawApplicationUseCase {

    private static final Logger log = LoggerFactory.getLogger(WithdrawApplicationService.class);

    private final UserRepositoryPort userRepositoryPort;
    private final JobRepositoryPort jobRepositoryPort;
    private final JobApplicationRepositoryPort jobApplicationRepositoryPort;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public JobApplication withdrawApplication(UUID userId, UUID jobId) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(jobId, "jobId");
        userRepositoryPort.findById(userId)
                .orElseThrow(() -> UserNotFoundException.byId(userId))
                .checkIsJobSeeker();

        // Only the user's own application: it is looked up by the user from the token and the job
        JobApplication application = jobApplicationRepositoryPort.findByUserAndJob(userId, jobId)
                .orElseThrow(() -> new JobApplicationNotFoundException(jobId));

        if (application.isWithdrawn()) {
            log.info("Application of user {} for job {} was already withdrawn", userId, jobId);
            return application;
        }

        // withdraw() refuses a rejected application
        JobApplication withdrawn = jobApplicationRepositoryPort.save(application.withdraw());
        jobRepositoryPort.changeApplicationsCount(jobId, -1);
        log.info("User {} withdrew the application for job {}", userId, jobId);
        return withdrawn;
    }
}

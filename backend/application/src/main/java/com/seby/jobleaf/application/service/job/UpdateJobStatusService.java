package com.seby.jobleaf.application.service.job;

import com.seby.jobleaf.domain.exceptions.job.JobNotFoundException;
import com.seby.jobleaf.domain.exceptions.user.UserNotFoundException;
import com.seby.jobleaf.domain.model.job.Job;
import com.seby.jobleaf.domain.model.job.JobStatus;
import com.seby.jobleaf.domain.port.in.job.UpdateJobStatusUseCase;
import com.seby.jobleaf.domain.port.out.auth.UserRepositoryPort;
import com.seby.jobleaf.domain.port.out.job.JobRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Application service implementing {@link UpdateJobStatusUseCase}.
 */
@Service
@RequiredArgsConstructor
public class UpdateJobStatusService implements UpdateJobStatusUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdateJobStatusService.class);

    private final UserRepositoryPort userRepositoryPort;
    private final JobRepositoryPort jobRepositoryPort;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Job updateStatus(UUID employerId, UUID jobId, JobStatus status) {
        Objects.requireNonNull(employerId, "employerId");
        Objects.requireNonNull(jobId, "jobId");
        Objects.requireNonNull(status, "status");
        UUID companyId = userRepositoryPort.findById(employerId)
                .orElseThrow(() -> UserNotFoundException.byId(employerId))
                .requireEmployerCompany();

        var job = jobRepositoryPort.findById(jobId)
                .filter(found -> found.belongsTo(companyId))
                .orElseThrow(() -> {
                    log.warn("Employer {} tried to change job {}, which is not in company {}", employerId, jobId,
                            companyId);
                    return new JobNotFoundException(jobId);
                });

        if (job.currentStatus() == status) {
            log.info("Job {} already is {}, nothing to change", jobId, status);
            return job;
        }

        var updated = jobRepositoryPort.updateStatus(job.withStatus(status, Instant.now()));
        log.info("Employer {} changed job {} from {} to {}", employerId, jobId, job.status(), updated.status());
        return updated;
    }
}

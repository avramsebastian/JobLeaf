package com.seby.jobleaf.application.service.job;

import com.seby.jobleaf.domain.exceptions.user.UserNotFoundException;
import com.seby.jobleaf.domain.model.job.Job;
import com.seby.jobleaf.domain.model.job.JobDetails;
import com.seby.jobleaf.domain.port.in.job.CreateJobUseCase;
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
 * Application service implementing {@link CreateJobUseCase}.
 */
@Service
@RequiredArgsConstructor
public class CreateJobService implements CreateJobUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateJobService.class);

    private final UserRepositoryPort userRepositoryPort;
    private final JobRepositoryPort jobRepositoryPort;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Job createJob(UUID employerId, JobDetails details) {
        Objects.requireNonNull(employerId, "employerId");
        Objects.requireNonNull(details, "details");
        UUID companyId = userRepositoryPort.findById(employerId)
                .orElseThrow(() -> UserNotFoundException.byId(employerId))
                .requireEmployerCompany();

        var created = jobRepositoryPort.create(Job.create(UUID.randomUUID(), companyId, details, Instant.now(),
                employerId));
        log.info("Employer {} created job {} ({}) as {} for company {}", employerId, created.id(), created.title(),
                created.status(), companyId);
        return created;
    }
}

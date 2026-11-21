package com.seby.jobleaf.application.service.job;

import com.seby.jobleaf.domain.exceptions.user.UserNotFoundException;
import com.seby.jobleaf.domain.model.job.Job;
import com.seby.jobleaf.domain.port.in.job.ListEmployerJobsUseCase;
import com.seby.jobleaf.domain.port.out.auth.UserRepositoryPort;
import com.seby.jobleaf.domain.port.out.job.JobRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Application service implementing {@link ListEmployerJobsUseCase}.
 */
@Service
@RequiredArgsConstructor
public class ListEmployerJobsService implements ListEmployerJobsUseCase {

    private static final Logger log = LoggerFactory.getLogger(ListEmployerJobsService.class);

    private final UserRepositoryPort userRepositoryPort;
    private final JobRepositoryPort jobRepositoryPort;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<Job> listJobs(UUID employerId) {
        Objects.requireNonNull(employerId, "employerId");
        UUID companyId = userRepositoryPort.findById(employerId)
                .orElseThrow(() -> UserNotFoundException.byId(employerId))
                .requireEmployerCompany();

        var jobs = jobRepositoryPort.findAllByCompanyId(companyId);
        log.info("Listed {} jobs of company {} for employer {}", jobs.size(), companyId, employerId);
        return jobs;
    }
}

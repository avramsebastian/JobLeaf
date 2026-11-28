package com.seby.jobleaf.application.service.jobapplication;

import com.seby.jobleaf.domain.exceptions.job.JobNotFoundException;
import com.seby.jobleaf.domain.exceptions.user.UserNotFoundException;
import com.seby.jobleaf.domain.model.jobapplication.ReceivedApplication;
import com.seby.jobleaf.domain.port.in.jobapplication.ListReceivedApplicationsUseCase;
import com.seby.jobleaf.domain.port.out.auth.UserRepositoryPort;
import com.seby.jobleaf.domain.port.out.job.JobRepositoryPort;
import com.seby.jobleaf.domain.port.out.jobapplication.JobApplicationRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Application service implementing {@link ListReceivedApplicationsUseCase}.
 */
@Service
@RequiredArgsConstructor
public class ListReceivedApplicationsService implements ListReceivedApplicationsUseCase {

    private static final Logger log = LoggerFactory.getLogger(ListReceivedApplicationsService.class);

    private final UserRepositoryPort userRepositoryPort;
    private final JobRepositoryPort jobRepositoryPort;
    private final JobApplicationRepositoryPort jobApplicationRepositoryPort;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<ReceivedApplication> listReceivedApplications(UUID employerId, UUID jobId) {
        Objects.requireNonNull(employerId, "employerId");
        Objects.requireNonNull(jobId, "jobId");
        UUID companyId = userRepositoryPort.findById(employerId)
                .orElseThrow(() -> UserNotFoundException.byId(employerId))
                .requireEmployerCompany();

        // A job of another company answers 404, like a job that does not exist: nothing about it leaks
        jobRepositoryPort.findById(jobId)
                .filter(job -> job.belongsTo(companyId))
                .orElseThrow(() -> {
                    log.warn("Employer {} tried to read the applications of job {}, which is not in company {}",
                            employerId, jobId, companyId);
                    return new JobNotFoundException(jobId);
                });

        List<ReceivedApplication> applications = jobApplicationRepositoryPort.findReceivedByJob(jobId);
        log.debug("Employer {} read {} applications of job {}", employerId, applications.size(), jobId);
        return applications;
    }
}

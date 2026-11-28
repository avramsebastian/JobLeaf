package com.seby.jobleaf.application.service.jobapplication;

import com.seby.jobleaf.domain.exceptions.jobapplication.JobApplicationNotFoundException;
import com.seby.jobleaf.domain.exceptions.user.UserNotFoundException;
import com.seby.jobleaf.domain.model.jobapplication.ApplicationStatus;
import com.seby.jobleaf.domain.model.jobapplication.ReceivedApplication;
import com.seby.jobleaf.domain.port.in.jobapplication.UpdateJobApplicationUseCase;
import com.seby.jobleaf.domain.port.out.auth.UserRepositoryPort;
import com.seby.jobleaf.domain.port.out.jobapplication.JobApplicationRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

/**
 * Application service implementing {@link UpdateJobApplicationUseCase}.
 */
@Service
@RequiredArgsConstructor
public class UpdateJobApplicationService implements UpdateJobApplicationUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdateJobApplicationService.class);

    private final UserRepositoryPort userRepositoryPort;
    private final JobApplicationRepositoryPort jobApplicationRepositoryPort;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ReceivedApplication updateJobApplication(UUID employerId, UUID applicationId, ApplicationStatus status,
                                                    String notes) {
        Objects.requireNonNull(employerId, "employerId");
        Objects.requireNonNull(applicationId, "applicationId");
        UUID companyId = userRepositoryPort.findById(employerId)
                .orElseThrow(() -> UserNotFoundException.byId(employerId))
                .requireEmployerCompany();

        // Only applications for the employer's own company: any other one answers 404
        ReceivedApplication received = jobApplicationRepositoryPort.findReceivedById(applicationId)
                .filter(found -> found.application().job().belongsTo(companyId))
                .orElseThrow(() -> {
                    log.warn("Employer {} tried to change application {}, which is not for company {}", employerId,
                            applicationId, companyId);
                    return JobApplicationNotFoundException.byId(applicationId);
                });

        // review() checks the status and the notes and refuses a withdrawn application
        var reviewed = received.application().review(status, notes);
        var saved = jobApplicationRepositoryPort.save(reviewed);
        log.info("Employer {} changed application {} from {} to {}", employerId, applicationId,
                received.application().status(), saved.status());
        return received.withApplication(saved);
    }
}

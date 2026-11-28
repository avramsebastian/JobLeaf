package com.seby.jobleaf.application.service.jobapplication;

import com.seby.jobleaf.domain.exceptions.user.UserNotFoundException;
import com.seby.jobleaf.domain.model.jobapplication.JobApplication;
import com.seby.jobleaf.domain.port.in.jobapplication.ListMyApplicationsUseCase;
import com.seby.jobleaf.domain.port.out.auth.UserRepositoryPort;
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
 * Application service implementing {@link ListMyApplicationsUseCase}.
 */
@Service
@RequiredArgsConstructor
public class ListMyApplicationsService implements ListMyApplicationsUseCase {

    private static final Logger log = LoggerFactory.getLogger(ListMyApplicationsService.class);

    private final UserRepositoryPort userRepositoryPort;
    private final JobApplicationRepositoryPort jobApplicationRepositoryPort;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<JobApplication> listMyApplications(UUID userId) {
        Objects.requireNonNull(userId, "userId");
        userRepositoryPort.findById(userId)
                .orElseThrow(() -> UserNotFoundException.byId(userId))
                .checkIsJobSeeker();

        var applications = jobApplicationRepositoryPort.findAllByUser(userId);
        log.info("Listed {} applications of user {}", applications.size(), userId);
        return applications;
    }
}

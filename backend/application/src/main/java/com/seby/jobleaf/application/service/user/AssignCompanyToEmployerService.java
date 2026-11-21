package com.seby.jobleaf.application.service.user;

import com.seby.jobleaf.domain.exceptions.company.CompanyNotFoundException;
import com.seby.jobleaf.domain.exceptions.user.UserNotFoundException;
import com.seby.jobleaf.domain.model.user.JobLeafUser;
import com.seby.jobleaf.domain.port.in.user.AssignCompanyToEmployerUseCase;
import com.seby.jobleaf.domain.port.out.auth.UserRepositoryPort;
import com.seby.jobleaf.domain.port.out.company.CompanyRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

/**
 * Application service implementing {@link AssignCompanyToEmployerUseCase}.
 */
@Service
@RequiredArgsConstructor
public class AssignCompanyToEmployerService implements AssignCompanyToEmployerUseCase {

    private static final Logger log = LoggerFactory.getLogger(AssignCompanyToEmployerService.class);

    private final UserRepositoryPort userRepositoryPort;
    private final CompanyRepositoryPort companyRepositoryPort;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public JobLeafUser assignCompany(UUID userId, UUID companyId) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(companyId, "companyId");

        var user = userRepositoryPort.findById(userId).orElseThrow(() -> {
            log.warn("Cannot assign company to user {}: user not found", userId);
            return UserNotFoundException.byId(userId);
        });
        user.checkCanBeAssignedToCompany();

        var company = companyRepositoryPort.findByIdWithoutJobs(companyId).orElseThrow(() -> {
            log.warn("Cannot assign company {} to user {}: company not found", companyId, userId);
            return new CompanyNotFoundException("Company with id " + companyId + " not found");
        });

        var assigned = userRepositoryPort.assignCompany(userId, companyId);
        log.info("Assigned company {} ({}) to employer {} (previous company: {})", companyId, company.name(), userId,
                user.companyId());
        return assigned;
    }
}

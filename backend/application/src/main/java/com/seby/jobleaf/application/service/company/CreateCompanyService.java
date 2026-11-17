package com.seby.jobleaf.application.service.company;

import com.seby.jobleaf.domain.exceptions.company.CompanyAlreadyExistsException;
import com.seby.jobleaf.domain.model.Company;
import com.seby.jobleaf.domain.model.CompanyDetails;
import com.seby.jobleaf.domain.model.SystemActor;
import com.seby.jobleaf.domain.port.in.company.CreateCompanyUseCase;
import com.seby.jobleaf.domain.port.out.company.CompanyRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

/**
 * Application service implementing {@link CreateCompanyUseCase}.
 */
@Service
@RequiredArgsConstructor
public class CreateCompanyService implements CreateCompanyUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateCompanyService.class);

    private final CompanyRepositoryPort companyRepositoryPort;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Company createCompany(CompanyDetails details) {
        Objects.requireNonNull(details, "details");
        if (companyRepositoryPort.existsByName(details.name())) {
            log.warn("Cannot create company: the name {} is taken", details.name());
            throw new CompanyAlreadyExistsException(details.name());
        }
        // createdBy is replaced by the logged-in admin through JPA auditing (CurrentActor)
        var created = companyRepositoryPort.create(Company.create(UUID.randomUUID(), details, SystemActor.ID));
        log.info("Created company {} ({})", created.id(), created.name());
        return created;
    }
}

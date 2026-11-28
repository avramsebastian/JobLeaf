package com.seby.jobleaf.application.service.company;

import com.seby.jobleaf.domain.exceptions.company.CompanyAlreadyExistsException;
import com.seby.jobleaf.domain.exceptions.company.CompanyNotFoundException;
import com.seby.jobleaf.domain.model.company.Company;
import com.seby.jobleaf.domain.model.company.CompanyDetails;
import com.seby.jobleaf.domain.port.in.company.UpdateCompanyUseCase;
import com.seby.jobleaf.domain.port.out.company.CompanyRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

/**
 * Application service implementing {@link UpdateCompanyUseCase}.
 */
@Service
@RequiredArgsConstructor
public class UpdateCompanyService implements UpdateCompanyUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdateCompanyService.class);

    private final CompanyRepositoryPort companyRepositoryPort;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Company updateCompany(UUID id, CompanyDetails details) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(details, "details");
        var company = companyRepositoryPort.findByIdWithoutJobs(id)
                .orElseThrow(() -> {
                    log.warn("Cannot update company {}: not found", id);
                    return new CompanyNotFoundException("Company with id " + id + " not found");
                });
        if (companyRepositoryPort.existsByNameExcluding(details.name(), id)) {
            log.warn("Cannot rename company {}: the name {} is taken", id, details.name());
            throw new CompanyAlreadyExistsException(details.name());
        }
        var updated = companyRepositoryPort.updateDetails(company.withDetails(details));
        log.info("Updated company {} ({})", id, updated.name());
        return updated;
    }
}

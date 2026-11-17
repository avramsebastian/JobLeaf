package com.seby.jobleaf.application.service.company;

import com.seby.jobleaf.domain.exceptions.company.CompanyNotFoundException;
import com.seby.jobleaf.domain.port.in.company.DeleteCompanyUseCase;
import com.seby.jobleaf.domain.port.out.company.CompanyRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

/**
 * Application service implementing {@link DeleteCompanyUseCase}.
 */
@Service
@RequiredArgsConstructor
public class DeleteCompanyService implements DeleteCompanyUseCase {

    private static final Logger log = LoggerFactory.getLogger(DeleteCompanyService.class);

    private final CompanyRepositoryPort companyRepositoryPort;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deleteCompany(UUID id) {
        Objects.requireNonNull(id, "id");
        var company = companyRepositoryPort.findByIdWithoutJobs(id)
                .orElseThrow(() -> {
                    log.warn("Cannot delete company {}: not found", id);
                    return new CompanyNotFoundException("Company with id " + id + " not found");
                });
        companyRepositoryPort.deleteById(id);
        log.info("Deleted company {} ({}) with its jobs", id, company.name());
    }
}

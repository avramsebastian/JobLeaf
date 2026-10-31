package com.seby.jobleaf.application.service.company;

import com.seby.jobleaf.domain.exceptions.company.CompanyNotFoundException;
import com.seby.jobleaf.domain.model.Company;
import com.seby.jobleaf.domain.port.in.company.ReadCompanyUseCase;
import com.seby.jobleaf.domain.port.out.company.CompanyRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Application service implementing {@link ReadCompanyUseCase}.
 */
@Service
@RequiredArgsConstructor
public class ReadCompanyService implements ReadCompanyUseCase {

    private static final Logger log = LoggerFactory.getLogger(ReadCompanyService.class);

    private final CompanyRepositoryPort companyRepositoryPort;


    @Override
    @Transactional(readOnly = true)
    public Company getCompanyById(UUID id) {
        log.debug("Reading company with id {}", id);
        var company = companyRepositoryPort.findById(id)
                .orElseThrow(() -> {
                    log.warn("Company with id {} not found", id);
                    return new CompanyNotFoundException("Company with id " + id + " not found");
                });
        log.info("Read company with id {} ({}) with {} jobs", id, company.name(), jobCount(company));
        return company;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Company> listAllCompanies() {
        log.debug("Listing all companies");
        var companies = companyRepositoryPort.findAll();
        if (companies.isEmpty()) {
            log.warn("Listed 0 companies, the companies table is empty");
            return companies;
        }
        long totalJobs = companies.stream().mapToLong(ReadCompanyService::jobCount).sum();
        log.info("Listed {} companies with {} jobs in total", companies.size(), totalJobs);
        return companies;
    }

    private static int jobCount(Company company) {
        return company.jobs() == null ? 0 : company.jobs().size();
    }
}

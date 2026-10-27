package com.seby.jobleaf.application.service.company;

import com.seby.jobleaf.domain.exceptions.CompanyNotFoundException;
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
        var company = companyRepositoryPort.findById(id)
                .orElseThrow(() -> new CompanyNotFoundException("Company with id " + id + " not found"));
        log.info("Read Company with id {} ", id);
        return company;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Company> listAllCompanies() {
        var companies = companyRepositoryPort.findAll();
        log.info("Listed {} companies", companies.size());
        return companies;
    }
}

package com.seby.jobleaf.domain.port.in.company;

import com.seby.jobleaf.domain.model.Company;

import java.util.List;
import java.util.UUID;

/**
 * Inbound port for reading companies.
 */
public interface ReadCompanyUseCase {

    /**
     * @param id company identifier
     * @return company domain object
     */
    Company getCompanyById(UUID id);

    /**
     * @return a list of companies
     */
    List<Company> listAllCompanies();
}

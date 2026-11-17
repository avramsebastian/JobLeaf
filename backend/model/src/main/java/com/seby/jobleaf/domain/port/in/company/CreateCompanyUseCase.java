package com.seby.jobleaf.domain.port.in.company;

import com.seby.jobleaf.domain.model.Company;
import com.seby.jobleaf.domain.model.CompanyDetails;

/**
 * Inbound port for the admin creating a company.
 */
public interface CreateCompanyUseCase {

    /**
     * @return the new company, without jobs (employers add them later)
     * @throws com.seby.jobleaf.domain.exceptions.company.CompanyAlreadyExistsException if the name is taken
     */
    Company createCompany(CompanyDetails details);
}

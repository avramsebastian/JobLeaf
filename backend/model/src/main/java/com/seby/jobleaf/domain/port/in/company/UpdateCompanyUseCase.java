package com.seby.jobleaf.domain.port.in.company;

import com.seby.jobleaf.domain.model.Company;
import com.seby.jobleaf.domain.model.CompanyDetails;

import java.util.UUID;

/**
 * Inbound port for the admin changing the details of a company.
 */
public interface UpdateCompanyUseCase {

    /**
     * Replaces all details of the company; its jobs are not touched.
     *
     * @return the updated company (its jobs are not loaded: empty list)
     * @throws com.seby.jobleaf.domain.exceptions.company.CompanyNotFoundException      if no company has this id
     * @throws com.seby.jobleaf.domain.exceptions.company.CompanyAlreadyExistsException if another company has the name
     */
    Company updateCompany(UUID id, CompanyDetails details);
}

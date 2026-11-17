package com.seby.jobleaf.domain.port.in.company;

import java.util.UUID;

/**
 * Inbound port for the admin deleting a company.
 */
public interface DeleteCompanyUseCase {

    /**
     * Deletes the company and its jobs. Employers assigned to it stay, without a company.
     *
     * @throws com.seby.jobleaf.domain.exceptions.company.CompanyNotFoundException if no company has this id
     */
    void deleteCompany(UUID id);
}

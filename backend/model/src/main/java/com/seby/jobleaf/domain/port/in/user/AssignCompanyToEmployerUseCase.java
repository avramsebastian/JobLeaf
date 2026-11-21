package com.seby.jobleaf.domain.port.in.user;

import com.seby.jobleaf.domain.model.user.JobLeafUser;

import java.util.UUID;

/**
 * Inbound port for the admin linking an employer to the company they work for.
 */
public interface AssignCompanyToEmployerUseCase {

    /**
     * Sets the employer's company (replacing a previous one).
     *
     * @return the employer with the company set
     * @throws com.seby.jobleaf.domain.exceptions.user.UserNotFoundException       if no user has this id
     * @throws com.seby.jobleaf.domain.exceptions.company.CompanyNotFoundException if no company has this id
     * @throws com.seby.jobleaf.domain.exceptions.user.UserRoleConflictException   if the user is not an employer
     */
    JobLeafUser assignCompany(UUID userId, UUID companyId);
}

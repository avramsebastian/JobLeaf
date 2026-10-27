package com.seby.jobleaf.domain.port.out.company;

import com.seby.jobleaf.domain.model.Company;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for persisting {@link Company} aggregates.
 *
 * <p>
 * Implementations live in the infrastructure layer (e.g. JPA adapters).
 */
public interface CompanyRepositoryPort {

    /**
     * Persists create for the given aggregate.
     *
     * @param company domain state to store
     */
    Company save(Company company);

    /**
     * Retrieves the company based on the id or null if not found.
     *
     * @param id company identifier
     * @return the company with the given identifier.
     */
    Optional<Company> findById(UUID id);

    /**
     * Retrieves all the companies that are stored.
     *
     * @return all the companies that are stored.
     */
    List<Company> findAll();
}

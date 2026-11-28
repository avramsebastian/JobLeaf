package com.seby.jobleaf.domain.port.out.company;

import com.seby.jobleaf.domain.model.company.Company;

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

    /**
     * @return every company sorted by name, without loading jobs ({@code jobs} is an empty list)
     */
    List<Company> findAllWithoutJobs();

    /**
     * @return the company without loading its jobs ({@code jobs} is an empty list)
     */
    Optional<Company> findByIdWithoutJobs(UUID id);

    /**
     * Creates a company. Its jobs list must be empty.
     *
     * @return the stored company, with audit fields filled in
     */
    Company create(Company company);

    /**
     * Stores the new details of an existing company. Only the details change: its jobs are left untouched (an
     * empty jobs list here does NOT delete them).
     *
     * @return the stored company (jobs not loaded: empty list)
     */
    Company updateDetails(Company company);

    /**
     * Deletes the company together with its jobs.
     */
    void deleteById(UUID id);

    /**
     * @return whether a company has this name, ignoring upper/lower case
     */
    boolean existsByName(String name);

    /**
     * @return whether a company other than {@code excludedId} has this name, ignoring upper/lower case
     */
    boolean existsByNameExcluding(String name, UUID excludedId);
}

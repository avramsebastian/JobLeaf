package com.seby.jobleaf.infrastructure.persistence.adapter;

import com.seby.jobleaf.domain.model.Company;
import com.seby.jobleaf.domain.port.out.company.CompanyRepositoryPort;
import com.seby.jobleaf.infrastructure.config.CacheConfig;
import com.seby.jobleaf.infrastructure.persistence.entity.CompanyEntity;
import com.seby.jobleaf.infrastructure.persistence.mapper.CompanyPersistenceMapper;
import com.seby.jobleaf.infrastructure.persistence.repository.CompanyJpaRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Secondary adapter that implements {@link CompanyRepositoryPort} using Spring Data JPA.
 * <p>
 * The public company list is cached ({@link CacheConfig#PUBLIC_COMPANIES}); every method that writes companies
 * clears that cache. When job writes are added (employers posting or closing jobs), they must clear it too,
 * because the cached list contains the jobs.
 */
@Component
@RequiredArgsConstructor
@Transactional
public class CompanyRepositoryAdapter implements CompanyRepositoryPort {

    private static final Logger log = LoggerFactory.getLogger(CompanyRepositoryAdapter.class);

    static final String NAME = "name";

    private final CompanyJpaRepository companyJpaRepository;
    private final CompanyPersistenceMapper mapper;

    @Override
    @CacheEvict(cacheNames = CacheConfig.PUBLIC_COMPANIES, allEntries = true)
    public Company save(Company company) {
        CompanyEntity saved = companyJpaRepository.save(mapper.toEntity(company));
        log.debug("Saved company with id {}", saved.getId());
        return mapper.toDomain(saved);
    }


    @Override
    @Transactional(readOnly = true)
    public Optional<Company> findById(UUID id) {
        long start = System.currentTimeMillis();
        var company = companyJpaRepository.findById(id).map(mapper::toDomain);
        log.debug("Loaded company with id {} from database (found={}) in {}ms",
                id, company.isPresent(), System.currentTimeMillis() - start);
        return company;
    }

    /**
     * Cached: only the first call (or the first after a change or after the time to live) runs the query, every
     * other visitor gets the same list from memory. The "Loaded ..." log line below therefore only appears on a
     * cache miss. Jobs are loaded lazily while mapping, so the duration includes the batched job queries.
     * The cached value is an immutable list of immutable records, so no caller can change it.
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.PUBLIC_COMPANIES, sync = true)
    public List<Company> findAll() {
        long start = System.currentTimeMillis();
        var companies = companyJpaRepository.findAllWithJobsByStatus("ACTIVE").stream().map(mapper::toDomain).toList();
        log.debug("Loaded {} companies with their jobs from database in {}ms",
                companies.size(), System.currentTimeMillis() - start);
        return companies;
    }
    @Override
    @Transactional(readOnly = true)
    public List<Company> findAllWithoutJobs() {
        return companyJpaRepository.findAll(Sort.by(NAME)).stream().map(mapper::toDomainWithoutJobs).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Company> findByIdWithoutJobs(UUID id) {
        return companyJpaRepository.findById(id).map(mapper::toDomainWithoutJobs);
    }

    /**
     * {@code createdAt} is empty, so {@code BaseEntity.isNew()} is true and JPA inserts (persist) instead of merging.
     */
    @Override
    @CacheEvict(cacheNames = CacheConfig.PUBLIC_COMPANIES, allEntries = true)
    public Company create(Company company) {
        CompanyEntity saved = companyJpaRepository.save(mapper.toEntity(company));
        log.debug("Created company with id {}", saved.getId());
        return mapper.toDomainWithoutJobs(saved);
    }

    /**
     * Loads the managed entity and copies only the details onto it. Saving a new entity built from the domain
     * object instead would replace the jobs collection, and orphanRemoval would then delete every job.
     * saveAndFlush: the UPDATE runs now, so auditing fills in updatedAt/updatedBy before the result is mapped.
     */
    @Override
    @CacheEvict(cacheNames = CacheConfig.PUBLIC_COMPANIES, allEntries = true)
    public Company updateDetails(Company company) {
        CompanyEntity entity = companyJpaRepository.findById(company.id())
                .orElseThrow(() -> new IllegalStateException("Company " + company.id() + " disappeared during update"));
        mapper.applyDetails(company, entity);
        CompanyEntity saved = companyJpaRepository.saveAndFlush(entity);
        log.debug("Updated company with id {}", saved.getId());
        return mapper.toDomainWithoutJobs(saved);
    }

    /**
     * Cascade (CascadeType.ALL + orphanRemoval) deletes the jobs first; the users.company_id foreign key is
     * ON DELETE SET NULL, so assigned employers keep their account without a company.
     */
    @Override
    @CacheEvict(cacheNames = CacheConfig.PUBLIC_COMPANIES, allEntries = true)
    public void deleteById(UUID id) {
        companyJpaRepository.deleteById(id);
        log.debug("Deleted company with id {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return companyJpaRepository.existsByNameIgnoreCase(name);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByNameExcluding(String name, UUID excludedId) {
        return companyJpaRepository.existsByNameIgnoreCaseAndIdNot(name, excludedId);
    }
}

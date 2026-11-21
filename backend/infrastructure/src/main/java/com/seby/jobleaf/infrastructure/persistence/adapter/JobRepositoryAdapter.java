package com.seby.jobleaf.infrastructure.persistence.adapter;

import com.seby.jobleaf.domain.model.job.Job;
import com.seby.jobleaf.domain.port.out.job.JobRepositoryPort;
import com.seby.jobleaf.infrastructure.config.CacheConfig;
import com.seby.jobleaf.infrastructure.persistence.entity.JobEntity;
import com.seby.jobleaf.infrastructure.persistence.mapper.JobPersistenceMapper;
import com.seby.jobleaf.infrastructure.persistence.repository.CompanyJpaRepository;
import com.seby.jobleaf.infrastructure.persistence.repository.JobJpaRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Secondary adapter that implements {@link JobRepositoryPort} using Spring Data JPA.
 * <p>
 * Every write clears the cached public company list ({@link CacheConfig#PUBLIC_COMPANIES}), because that list
 * contains the active jobs.
 */
@Component
@RequiredArgsConstructor
@Transactional
public class JobRepositoryAdapter implements JobRepositoryPort {

    private static final Logger log = LoggerFactory.getLogger(JobRepositoryAdapter.class);

    private final JobJpaRepository jobJpaRepository;
    private final CompanyJpaRepository companyJpaRepository;
    private final JobPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<Job> findAllByCompanyId(UUID companyId) {
        return jobJpaRepository.findByCompany_IdOrderByCreatedAtDesc(companyId).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Job> findById(UUID id) {
        return jobJpaRepository.findById(id).map(mapper::toDomain);
    }

    /**
     * getReferenceById: the job only needs the company's id for the foreign key, so no SELECT of the company runs.
     */
    @Override
    @CacheEvict(cacheNames = CacheConfig.PUBLIC_COMPANIES, allEntries = true)
    public Job create(Job job) {
        JobEntity saved = jobJpaRepository.save(
                mapper.toEntity(job, companyJpaRepository.getReferenceById(job.companyId())));
        log.debug("Created job {} for company {}", saved.getId(), job.companyId());
        return mapper.toDomain(saved);
    }

    /**
     * Changes only status and posted date on the managed entity. saveAndFlush: the UPDATE runs now, so auditing fills
     * in updatedAt/updatedBy before the result is mapped.
     */
    @Override
    @CacheEvict(cacheNames = CacheConfig.PUBLIC_COMPANIES, allEntries = true)
    public Job updateStatus(Job job) {
        JobEntity entity = jobJpaRepository.findById(job.id())
                .orElseThrow(() -> new IllegalStateException("Job " + job.id() + " disappeared during the update"));
        entity.setStatus(job.status());
        entity.setPostedDate(job.postedDate());
        JobEntity saved = jobJpaRepository.saveAndFlush(entity);
        log.debug("Changed status of job {} to {}", saved.getId(), saved.getStatus());
        return mapper.toDomain(saved);
    }
}

package com.seby.jobleaf.infrastructure.persistence.adapter;

import com.seby.jobleaf.domain.model.job.Job;
import com.seby.jobleaf.domain.model.job.JobDetails;
import com.seby.jobleaf.domain.model.job.JobStatus;
import com.seby.jobleaf.domain.model.user.SystemActor;
import com.seby.jobleaf.domain.port.out.job.JobRepositoryPort;
import com.seby.jobleaf.infrastructure.config.CacheConfig;
import com.seby.jobleaf.infrastructure.persistence.entity.CompanyEntity;
import com.seby.jobleaf.infrastructure.persistence.entity.JobEntity;
import com.seby.jobleaf.infrastructure.persistence.mapper.JobPersistenceMapper;
import com.seby.jobleaf.infrastructure.persistence.repository.CompanyJpaRepository;
import com.seby.jobleaf.infrastructure.persistence.repository.JobJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Job writes must clear the cached public company list, because it contains the jobs.
 */
@SpringJUnitConfig
@Import({CacheConfig.class, JobRepositoryAdapter.class, JobPersistenceMapper.class})
class JobRepositoryAdapterCacheTest {

    @Autowired
    private JobRepositoryPort adapter;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private JobJpaRepository jobJpaRepository;

    @MockitoBean
    private CompanyJpaRepository companyJpaRepository;

    private Cache cache;
    private final UUID companyId = UUID.randomUUID();
    private Job job;

    @BeforeEach
    void setUp() {
        cache = cacheManager.getCache(CacheConfig.PUBLIC_COMPANIES);
        cache.put("any", List.of("cached companies"));
        var details = new JobDetails("Frontend Developer", "Bucharest", "Hybrid", "Full-time", "Technology",
                "Mid Level", BigDecimal.ONE, BigDecimal.TEN, "EUR", "year", "We are looking for a developer.",
                List.of("React"), List.of("Remote Work"), Instant.now().plus(Duration.ofDays(30)), false, false, false);
        job = Job.create(UUID.randomUUID(), companyId, details, Instant.now(), SystemActor.ID);
        var company = new CompanyEntity();
        company.setId(companyId);
        when(companyJpaRepository.getReferenceById(companyId)).thenReturn(company);
    }

    @Test
    @DisplayName("Should clear the public company cache when a job is created")
    void create_evicts() {
        when(jobJpaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        adapter.create(job);

        assertThat(cache.get("any")).isNull();
    }

    @Test
    @DisplayName("Should clear the public company cache when a job status changes")
    void updateStatus_evicts() {
        JobEntity entity = new JobPersistenceMapper().toEntity(job, companyJpaRepository.getReferenceById(companyId));
        when(jobJpaRepository.findById(job.id())).thenReturn(Optional.of(entity));
        when(jobJpaRepository.saveAndFlush(entity)).thenReturn(entity);

        Job updated = adapter.updateStatus(job.withStatus(JobStatus.ACTIVE, Instant.now()));

        assertThat(updated.status()).isEqualTo("ACTIVE");
        assertThat(cache.get("any")).isNull();
    }
}

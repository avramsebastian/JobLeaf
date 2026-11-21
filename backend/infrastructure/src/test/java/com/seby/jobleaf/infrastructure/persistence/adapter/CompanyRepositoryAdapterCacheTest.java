package com.seby.jobleaf.infrastructure.persistence.adapter;

import com.seby.jobleaf.domain.model.company.Company;
import com.seby.jobleaf.domain.model.company.CompanyDetails;
import com.seby.jobleaf.domain.model.user.SystemActor;
import com.seby.jobleaf.domain.port.out.company.CompanyRepositoryPort;
import com.seby.jobleaf.infrastructure.config.CacheConfig;
import com.seby.jobleaf.infrastructure.persistence.entity.CompanyEntity;
import com.seby.jobleaf.infrastructure.persistence.mapper.CompanyPersistenceMapper;
import com.seby.jobleaf.infrastructure.persistence.mapper.JobPersistenceMapper;
import com.seby.jobleaf.infrastructure.persistence.repository.CompanyJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The cache only works through Spring's proxy around the adapter, so this test uses a small Spring context
 * (the real {@link CacheConfig} and adapter, a mocked JPA repository) instead of a plain unit test.
 */
@SpringJUnitConfig
@Import({CacheConfig.class, CompanyRepositoryAdapter.class, CompanyPersistenceMapper.class})
class CompanyRepositoryAdapterCacheTest {

    private static final UUID ID = UUID.randomUUID();

    @Autowired
    private CompanyRepositoryPort adapter;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private CompanyJpaRepository companyJpaRepository;

    @MockitoBean
    private JobPersistenceMapper jobPersistenceMapper;

    private final CompanyEntity entity = new CompanyEntity();

    @BeforeEach
    void setUp() {
        cacheManager.getCache(CacheConfig.PUBLIC_COMPANIES).clear();
        entity.setId(ID);
        entity.setName("DerpCorp");
        when(companyJpaRepository.findAllWithJobsByStatus("ACTIVE")).thenReturn(List.of(entity));
    }

    @Test
    @DisplayName("Should query the database once and serve the next calls from the cache")
    void findAll_cached() {
        List<Company> first = adapter.findAll();
        List<Company> second = adapter.findAll();

        assertThat(second).isSameAs(first);
        verify(companyJpaRepository, times(1)).findAllWithJobsByStatus("ACTIVE");
    }

    @Test
    @DisplayName("Should run the query only once when many requests miss the cache at the same time (sync)")
    void findAll_concurrentMisses_loadOnce() throws Exception {
        // A slow query, so all threads arrive while the first one is still loading
        when(companyJpaRepository.findAllWithJobsByStatus("ACTIVE")).thenAnswer(invocation -> {
            Thread.sleep(300);
            return List.of(entity);
        });

        var pool = java.util.concurrent.Executors.newFixedThreadPool(8);
        try {
            var results = new java.util.ArrayList<java.util.concurrent.Future<List<Company>>>();
            for (int i = 0; i < 8; i++) {
                results.add(pool.submit(adapter::findAll));
            }
            for (var result : results) {
                assertThat(result.get()).hasSize(1);
            }
        } finally {
            pool.shutdownNow();
        }

        verify(companyJpaRepository, times(1)).findAllWithJobsByStatus("ACTIVE");
    }

    @Test
    @DisplayName("Should query again after a company update cleared the cache")
    void updateDetails_evicts() {
        adapter.findAll();
        when(companyJpaRepository.findById(ID)).thenReturn(Optional.of(entity));
        when(companyJpaRepository.saveAndFlush(entity)).thenReturn(entity);

        adapter.updateDetails(Company.create(ID, new CompanyDetails("New Name", "/l.png", "Technology", "Large",
                4.0, "Bucharest", 2010, "A company", 10, "https://n.example"), SystemActor.ID));
        adapter.findAll();

        verify(companyJpaRepository, times(2)).findAllWithJobsByStatus("ACTIVE");
    }

    @Test
    @DisplayName("Should also clear the cache on create and delete")
    void createAndDelete_evict() {
        adapter.findAll();
        when(companyJpaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        adapter.create(Company.create(UUID.randomUUID(), new CompanyDetails("Other", "/o.png", "Retail", "Small",
                3.0, "Cluj", 2000, "Other company", 5, "https://o.example"), SystemActor.ID));
        adapter.findAll();
        verify(companyJpaRepository, times(2)).findAllWithJobsByStatus("ACTIVE");

        clearInvocations(companyJpaRepository);
        adapter.deleteById(ID);
        adapter.findAll();
        verify(companyJpaRepository, times(1)).findAllWithJobsByStatus("ACTIVE");
    }

    @Test
    @DisplayName("Should not cache the admin reads, which must always be fresh")
    void adminList_notCached() {
        when(companyJpaRepository.findAll(org.springframework.data.domain.Sort.by("name"))).thenReturn(List.of(entity));

        adapter.findAllWithoutJobs();
        adapter.findAllWithoutJobs();

        verify(companyJpaRepository, times(2)).findAll(org.springframework.data.domain.Sort.by("name"));
    }
}

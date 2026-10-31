package com.seby.jobleaf.infrastructure.persistence.adapter;

import com.seby.jobleaf.domain.model.Company;
import com.seby.jobleaf.domain.port.out.company.CompanyRepositoryPort;
import com.seby.jobleaf.infrastructure.persistence.entity.CompanyEntity;
import com.seby.jobleaf.infrastructure.persistence.mapper.CompanyPersistenceMapper;
import com.seby.jobleaf.infrastructure.persistence.repository.CompanyJpaRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Secondary adapter that implements {@link CompanyRepositoryPort} using Spring Data JPA.
 */
@Component
@RequiredArgsConstructor
@Transactional
public class CompanyRepositoryAdapter implements CompanyRepositoryPort {

    private static final Logger log = LoggerFactory.getLogger(CompanyRepositoryAdapter.class);

    private final CompanyJpaRepository companyJpaRepository;
    private final CompanyPersistenceMapper mapper;

    @Override
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
     * Jobs are loaded lazily while mapping, so the duration includes the batched job queries.
     */
    @Override
    @Transactional(readOnly = true)
    public List<Company> findAll() {
        long start = System.currentTimeMillis();
        var companies = companyJpaRepository.findAll().stream().map(mapper::toDomain).toList();
        log.debug("Loaded {} companies with their jobs from database in {}ms",
                companies.size(), System.currentTimeMillis() - start);
        return companies;
    }
}

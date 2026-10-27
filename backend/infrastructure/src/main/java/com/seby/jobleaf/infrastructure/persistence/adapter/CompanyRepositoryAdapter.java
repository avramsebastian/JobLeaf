package com.seby.jobleaf.infrastructure.persistence.adapter;

import com.seby.jobleaf.domain.model.Company;
import com.seby.jobleaf.domain.port.out.company.CompanyRepositoryPort;
import com.seby.jobleaf.infrastructure.persistence.entity.CompanyEntity;
import com.seby.jobleaf.infrastructure.persistence.mapper.CompanyPersistenceMapper;
import com.seby.jobleaf.infrastructure.persistence.repository.CompanyJpaRepository;
import lombok.RequiredArgsConstructor;
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

    private final CompanyJpaRepository companyJpaRepository;
    private final CompanyPersistenceMapper mapper;

    @Override
    public Company save(Company company) {
        CompanyEntity saved = companyJpaRepository.save(mapper.toEntity(company));
        return mapper.toDomain(saved);
    }


    @Override
    @Transactional(readOnly = true)
    public Optional<Company> findById(UUID id) {
        return companyJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Company> findAll() {
        return companyJpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }
}

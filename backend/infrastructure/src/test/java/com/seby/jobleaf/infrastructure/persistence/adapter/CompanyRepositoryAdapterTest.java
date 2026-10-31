package com.seby.jobleaf.infrastructure.persistence.adapter;

import com.seby.jobleaf.domain.model.Company;
import com.seby.jobleaf.infrastructure.persistence.entity.CompanyEntity;
import com.seby.jobleaf.infrastructure.persistence.mapper.CompanyPersistenceMapper;
import com.seby.jobleaf.infrastructure.persistence.repository.CompanyJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyRepositoryAdapterTest {

    @Mock
    private CompanyJpaRepository companyJpaRepository;

    @Mock
    private CompanyPersistenceMapper mapper;

    @InjectMocks
    private CompanyRepositoryAdapter adapter;

    private Company createSampleCompany(UUID id, String name) {
        return new Company(
                id,
                name,
                "/logos/test.png",
                "Finance",
                "Large",
                4.7,
                "New York",
                2010,
                "Financial technology services",
                1200,
                "https://fintech.example",
                Instant.now(),
                UUID.randomUUID(),
                Instant.now(),
                UUID.randomUUID(),
                List.of()
        );
    }

    @Test
    @DisplayName("Should convert domain to entity, save to repository, and convert back to domain")
    void save_mapsToEntitySavesAndMapsToDomain() {
        // Given
        UUID id = UUID.randomUUID();
        Company domainCompany = createSampleCompany(id, "Fintech Corp");
        CompanyEntity entityToSave = new CompanyEntity();
        CompanyEntity savedEntity = new CompanyEntity();
        savedEntity.setId(id);

        when(mapper.toEntity(domainCompany)).thenReturn(entityToSave);
        when(companyJpaRepository.save(entityToSave)).thenReturn(savedEntity);
        when(mapper.toDomain(savedEntity)).thenReturn(domainCompany);

        // When
        Company result = adapter.save(domainCompany);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(id);
        assertThat(result.name()).isEqualTo("Fintech Corp");
        verify(mapper).toEntity(domainCompany);
        verify(companyJpaRepository).save(entityToSave);
        verify(mapper).toDomain(savedEntity);
    }

    @Test
    @DisplayName("Should find company by id and map to domain when entity exists")
    void findById_whenFound_returnsMappedCompany() {
        // Given
        UUID id = UUID.randomUUID();
        Company expectedCompany = createSampleCompany(id, "Fintech Corp");
        CompanyEntity entity = new CompanyEntity();
        entity.setId(id);

        when(companyJpaRepository.findById(id)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(expectedCompany);

        // When
        Optional<Company> result = adapter.findById(id);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(expectedCompany);
        verify(companyJpaRepository).findById(id);
        verify(mapper).toDomain(entity);
    }

    @Test
    @DisplayName("Should return empty Optional when company is not found by id")
    void findById_whenNotFound_returnsEmpty() {
        // Given
        UUID id = UUID.randomUUID();
        when(companyJpaRepository.findById(id)).thenReturn(Optional.empty());

        // When
        Optional<Company> result = adapter.findById(id);

        // Then
        assertThat(result).isEmpty();
        verify(companyJpaRepository).findById(id);
    }

    @Test
    @DisplayName("Should find all companies and map each to domain")
    void findAll_returnsListOfMappedCompanies() {
        // Given
        CompanyEntity entity1 = new CompanyEntity();
        CompanyEntity entity2 = new CompanyEntity();
        Company company1 = createSampleCompany(UUID.randomUUID(), "Company 1");
        Company company2 = createSampleCompany(UUID.randomUUID(), "Company 2");

        when(companyJpaRepository.findAll()).thenReturn(List.of(entity1, entity2));
        when(mapper.toDomain(entity1)).thenReturn(company1);
        when(mapper.toDomain(entity2)).thenReturn(company2);

        // When
        List<Company> results = adapter.findAll();

        // Then
        assertThat(results).hasSize(2);
        assertThat(results).containsExactly(company1, company2);
        verify(companyJpaRepository).findAll();
        verify(mapper).toDomain(entity1);
        verify(mapper).toDomain(entity2);
    }
}

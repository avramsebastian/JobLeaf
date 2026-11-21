package com.seby.jobleaf.infrastructure.persistence.adapter;

import com.seby.jobleaf.domain.model.company.Company;
import com.seby.jobleaf.domain.model.company.CompanyDetails;
import com.seby.jobleaf.domain.model.user.SystemActor;
import com.seby.jobleaf.infrastructure.persistence.entity.CompanyEntity;
import com.seby.jobleaf.infrastructure.persistence.entity.JobEntity;
import com.seby.jobleaf.infrastructure.persistence.mapper.CompanyPersistenceMapper;
import com.seby.jobleaf.infrastructure.persistence.mapper.JobPersistenceMapper;
import com.seby.jobleaf.infrastructure.persistence.repository.CompanyJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Uses the real persistence mapper: the point is what happens to the entity, especially its jobs.
 */
@ExtendWith(MockitoExtension.class)
class CompanyRepositoryAdapterAdminTest {

    private static final UUID ID = UUID.randomUUID();

    @Mock
    private CompanyJpaRepository companyJpaRepository;

    @Mock
    private JobPersistenceMapper jobMapper;

    private CompanyRepositoryAdapter adapter() {
        return new CompanyRepositoryAdapter(companyJpaRepository, new CompanyPersistenceMapper(jobMapper));
    }

    @Test
    @DisplayName("Should update only the details on the managed entity and leave its jobs alone")
    void updateDetails_keepsJobs() {
        var entity = new CompanyEntity();
        entity.setId(ID);
        entity.setName("Old Name");
        JobEntity job = mock(JobEntity.class);
        entity.getJobs().add(job);
        when(companyJpaRepository.findById(ID)).thenReturn(Optional.of(entity));
        when(companyJpaRepository.saveAndFlush(entity)).thenReturn(entity);

        var details = new CompanyDetails("New Name", "/logos/n.png", "Technology", "Large", 4.0, "Bucharest", 2010,
                "New", 10, "https://n.example");
        Company result = adapter().updateDetails(Company.create(ID, details, SystemActor.ID));

        assertThat(entity.getName()).isEqualTo("New Name");
        assertThat(entity.getJobs()).as("jobs are not replaced, so orphanRemoval deletes nothing").containsExactly(job);
        assertThat(result.name()).isEqualTo("New Name");
        assertThat(result.jobs()).isEmpty();
        verifyNoInteractions(jobMapper);
    }

    @Test
    @DisplayName("Should list all companies sorted by name without touching their jobs")
    void findAllWithoutJobs_sortedByName() {
        var entity = new CompanyEntity();
        entity.setName("A");
        when(companyJpaRepository.findAll(Sort.by("name"))).thenReturn(List.of(entity));

        assertThat(adapter().findAllWithoutJobs()).extracting(Company::name).containsExactly("A");
        verifyNoInteractions(jobMapper);
    }

    @Test
    @DisplayName("Should check name uniqueness ignoring case, excluding the company itself on update")
    void nameChecks_delegateToRepository() {
        when(companyJpaRepository.existsByNameIgnoreCase("derpcorp")).thenReturn(true);
        when(companyJpaRepository.existsByNameIgnoreCaseAndIdNot("derpcorp", ID)).thenReturn(false);

        assertThat(adapter().existsByName("derpcorp")).isTrue();
        assertThat(adapter().existsByNameExcluding("derpcorp", ID)).isFalse();
    }

    @Test
    @DisplayName("Should delete by id")
    void deleteById_delegates() {
        adapter().deleteById(ID);
        verify(companyJpaRepository).deleteById(ID);
    }
}

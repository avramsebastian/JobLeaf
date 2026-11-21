package com.seby.jobleaf.infrastructure.persistence.mapper;

import com.seby.jobleaf.domain.model.company.Company;
import com.seby.jobleaf.infrastructure.persistence.entity.CompanyEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CompanyPersistenceMapper {

    private final JobPersistenceMapper jobMapper;

    /**
     * Maps the company together with its lazily loaded jobs, so it must be called inside an open transaction.
     */
    public Company toDomain(CompanyEntity entity) {
        if(entity == null){
            return null;
        }

        return new Company(
                entity.getId(),
                entity.getName(),
                entity.getLogo(),
                entity.getIndustry(),
                entity.getSize(),
                entity.getRating(),
                entity.getLocations(),
                entity.getFounded(),
                entity.getDescription(),
                entity.getEmployees(),
                entity.getWebsite(),
                entity.getCreatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedAt(),
                entity.getUpdatedBy(),
                entity.getJobs().stream().map(jobMapper::toDomain).toList()
        );
    }

    /**
     * Maps only the company itself: the lazy {@code jobs} collection is never touched, so no job query runs.
     * {@code jobs} in the result is an empty list, meaning "not loaded".
     */
    public Company toDomainWithoutJobs(CompanyEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Company(
                entity.getId(),
                entity.getName(),
                entity.getLogo(),
                entity.getIndustry(),
                entity.getSize(),
                entity.getRating(),
                entity.getLocations(),
                entity.getFounded(),
                entity.getDescription(),
                entity.getEmployees(),
                entity.getWebsite(),
                entity.getCreatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedAt(),
                entity.getUpdatedBy(),
                List.of()
        );
    }

    /**
     * Copies the details an admin can change onto the entity (no id, audit fields or jobs).
     */
    public void applyDetails(Company company, CompanyEntity entity) {
        entity.setName(company.name());
        entity.setLogo(company.logo());
        entity.setIndustry(company.industry());
        entity.setSize(company.size());
        entity.setRating(company.rating());
        entity.setLocations(company.locations());
        entity.setFounded(company.founded());
        entity.setDescription(company.description());
        entity.setEmployees(company.employees());
        entity.setWebsite(company.website());
    }

    public CompanyEntity toEntity(Company company) {
        if (company == null) {
            return null;
        }

        CompanyEntity entity = new CompanyEntity();
        entity.setId(company.id());
        applyDetails(company, entity);
        entity.restoreAuditFields(company.createdAt(), company.createdBy(), company.updatedAt(), company.updatedBy());
        if (company.jobs() != null) {
            entity.getJobs().addAll(company.jobs().stream().map(job -> jobMapper.toEntity(job, entity)).toList());
        }

        return entity;
    }
}

package com.seby.jobleaf.infrastructure.persistence.mapper;

import com.seby.jobleaf.domain.model.Company;
import com.seby.jobleaf.infrastructure.persistence.entity.CompanyEntity;
import org.springframework.stereotype.Component;

@Component
public class CompanyPersistenceMapper {

    public Company toDomain(CompanyEntity entity) {
        return new Company(
                entity.id,
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
                entity.createdAt,
                entity.createdBy,
                entity.updatedAt,
                entity.updatedBy
        );
    }

    public CompanyEntity toEntity(Company company) {
        if (company == null) {
            return null;
        }

        CompanyEntity entity = new CompanyEntity();
        entity.id = company.id();
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
        entity.createdAt = company.createdAt();
        entity.createdBy = company.createdBy();
        entity.updatedAt = company.updatedAt();
        entity.updatedBy = company.updatedBy();

        return entity;
    }
}

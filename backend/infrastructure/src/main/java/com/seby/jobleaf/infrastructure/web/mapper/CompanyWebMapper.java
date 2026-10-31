package com.seby.jobleaf.infrastructure.web.mapper;

import com.seby.jobleaf.apis.generated.model.CompanyResponse;
import com.seby.jobleaf.domain.model.Company;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static java.util.Optional.ofNullable;

/**
 * Maps between domain {@link Company} aggregates and OpenAPI company DTOs (responses and draft-patch commands).
 */
@Component
@RequiredArgsConstructor
public class CompanyWebMapper {

    private final JobWebMapper jobWebMapper;

    public CompanyResponse toCompanyResponse(Company company) {
        var response = new CompanyResponse();
        response.setId(company.id());
        response.setName(company.name());
        response.setLogo(company.logo());
        response.setIndustry(company.industry());
        response.setSize(company.size());
        response.setRating(company.rating());
        response.setLocations(company.locations());
        response.setFounded(company.founded());
        response.setDescription(company.description());
        response.setEmployees(company.employees());
        response.setWebsite(company.website());
        response.setCreatedAt(convertInstant(company.createdAt()));
        response.setCreatedBy(company.createdBy());
        response.setUpdatedAt(convertInstant(company.updatedAt()));
        response.setUpdatedBy(company.updatedBy());
        response.setJobs(jobWebMapper.toListJobsResponse(company.jobs()));
        return response;
    }

    public List<CompanyResponse> toListCompaniesResponse(List<Company> companies) {
        return companies.stream().map(this::toCompanyResponse).toList();
    }

    private OffsetDateTime convertInstant(Instant instant) {
        return ofNullable(instant).map(date -> date.atOffset(ZoneOffset.UTC))
                .orElse(null);
    }
}

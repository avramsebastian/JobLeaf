package com.seby.jobleaf.infrastructure.web.mapper;

import com.seby.jobleaf.apis.generated.model.JobResponse;
import com.seby.jobleaf.domain.model.Job;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static java.util.Optional.ofNullable;

/**
 * Maps between domain {@link Job} objects and OpenAPI job DTOs.
 */
@Component
public class JobWebMapper {

    public JobResponse toJobResponse(Job job) {
        var response = new JobResponse();
        response.setId(job.id());
        response.setCompanyId(job.companyId());
        response.setTitle(job.title());
        response.setLocation(job.location());
        response.setWorkType(job.workType());
        response.setJobType(job.jobType());
        response.setCategory(job.category());
        response.setExperienceLevel(job.experienceLevel());
        response.setSalaryMin(job.salaryMin());
        response.setSalaryMax(job.salaryMax());
        response.setSalaryCurrency(job.salaryCurrency());
        response.setSalaryPeriod(job.salaryPeriod());
        response.setDescription(job.description());
        response.setRequirements(job.requirements());
        response.setBenefits(job.benefits());
        response.setPostedDate(convertInstant(job.postedDate()));
        response.setApplicationDeadline(convertInstant(job.applicationDeadline()));
        response.setApplicationsCount(job.applicationsCount());
        response.setFeatured(job.featured());
        response.setUrgent(job.urgent());
        response.setRemote(job.remote());
        response.setStatus(job.status());
        response.setCreatedAt(convertInstant(job.createdAt()));
        response.setCreatedBy(job.createdBy());
        response.setUpdatedAt(convertInstant(job.updatedAt()));
        response.setUpdatedBy(job.updatedBy());
        return response;
    }

    public List<JobResponse> toListJobsResponse(List<Job> jobs) {
        return ofNullable(jobs).map(list -> list.stream().map(this::toJobResponse).toList())
                .orElse(null);
    }

    private OffsetDateTime convertInstant(Instant instant) {
        return ofNullable(instant).map(date -> date.atOffset(ZoneOffset.UTC))
                .orElse(null);
    }
}

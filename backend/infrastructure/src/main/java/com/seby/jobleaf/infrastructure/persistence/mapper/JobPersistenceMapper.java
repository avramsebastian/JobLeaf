package com.seby.jobleaf.infrastructure.persistence.mapper;

import com.seby.jobleaf.domain.model.Job;
import com.seby.jobleaf.infrastructure.persistence.entity.CompanyEntity;
import com.seby.jobleaf.infrastructure.persistence.entity.JobEntity;
import org.springframework.stereotype.Component;

@Component
public class JobPersistenceMapper {

    public Job toDomain(JobEntity entity) {
        if (entity == null) {
            return null;
        }

        return new Job(
                entity.getId(),
                entity.getCompany() == null ? null : entity.getCompany().getId(),
                entity.getTitle(),
                entity.getLocation(),
                entity.getWorkType(),
                entity.getJobType(),
                entity.getCategory(),
                entity.getExperienceLevel(),
                entity.getSalaryMin(),
                entity.getSalaryMax(),
                entity.getSalaryCurrency(),
                entity.getSalaryPeriod(),
                entity.getDescription(),
                entity.getRequirements(),
                entity.getBenefits(),
                entity.getPostedDate(),
                entity.getApplicationDeadline(),
                entity.getApplicationsCount(),
                entity.isFeatured(),
                entity.isUrgent(),
                entity.isRemote(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedAt(),
                entity.getUpdatedBy()
        );
    }

    /**
     * @param job     domain job
     * @param company owning company entity, set as the back-reference of the relation
     */
    public JobEntity toEntity(Job job, CompanyEntity company) {
        if (job == null) {
            return null;
        }

        JobEntity entity = new JobEntity();
        entity.setId(job.id());
        entity.setCompany(company);
        entity.setTitle(job.title());
        entity.setLocation(job.location());
        entity.setWorkType(job.workType());
        entity.setJobType(job.jobType());
        entity.setCategory(job.category());
        entity.setExperienceLevel(job.experienceLevel());
        entity.setSalaryMin(job.salaryMin());
        entity.setSalaryMax(job.salaryMax());
        entity.setSalaryCurrency(job.salaryCurrency());
        entity.setSalaryPeriod(job.salaryPeriod());
        entity.setDescription(job.description());
        entity.setRequirements(job.requirements());
        entity.setBenefits(job.benefits());
        entity.setPostedDate(job.postedDate());
        entity.setApplicationDeadline(job.applicationDeadline());
        entity.setApplicationsCount(job.applicationsCount());
        entity.setFeatured(job.featured());
        entity.setUrgent(job.urgent());
        entity.setRemote(job.remote());
        entity.setStatus(job.status());
        entity.restoreAuditFields(job.createdAt(), job.createdBy(), job.updatedAt(), job.updatedBy());

        return entity;
    }
}

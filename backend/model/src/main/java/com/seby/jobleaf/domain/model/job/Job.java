package com.seby.jobleaf.domain.model.job;

import com.seby.jobleaf.domain.model.company.Company;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Immutable job posting belonging to a {@link Company}.
 */
public record Job(
        UUID id,
        UUID companyId,
        String title,
        String location,
        String workType,
        String jobType,
        String category,
        String experienceLevel,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String salaryCurrency,
        String salaryPeriod,
        String description,
        List<String> requirements,
        List<String> benefits,
        Instant postedDate,
        Instant applicationDeadline,
        Integer applicationsCount,
        boolean featured,
        boolean urgent,
        boolean remote,
        String status,
        Instant createdAt,
        UUID createdBy,
        Instant updatedAt,
        UUID updatedBy
) {

    /**
     * A new job of the given company, always in status DRAFT and without applications. postedDate is required by the
     * database, so it starts as the creation time and is set again when the job is published.
     */
    public static Job create(UUID id, UUID companyId, JobDetails details, Instant now, UUID createdBy) {
        return new Job(id, companyId, details.title(), details.location(), details.workType(), details.jobType(),
                details.category(), details.experienceLevel(), details.salaryMin(), details.salaryMax(),
                details.salaryCurrency(), details.salaryPeriod(), details.description(), details.requirements(),
                details.benefits(), now, details.applicationDeadline(), 0, details.featured(), details.urgent(),
                details.remote(), JobStatus.DRAFT.name(), null, createdBy, null, createdBy);
    }

    public JobStatus currentStatus() {
        return JobStatus.from(status);
    }

    /**
     * @return true if this job is owned by the given company
     */
    public boolean belongsTo(UUID otherCompanyId) {
        return companyId != null && companyId.equals(otherCompanyId);
    }

    /**
     * @return a copy with the new status; publishing a draft (DRAFT to ACTIVE) also sets postedDate to now, so job
     * seekers see when it really went online
     * @throws com.seby.jobleaf.domain.exceptions.job.InvalidJobStatusChangeException when going back to DRAFT
     */
    public Job withStatus(JobStatus target, Instant now) {
        JobStatus current = currentStatus();
        current.checkCanChangeTo(target);
        Instant posted = current == JobStatus.DRAFT && target == JobStatus.ACTIVE ? now : postedDate;
        return new Job(id, companyId, title, location, workType, jobType, category, experienceLevel, salaryMin,
                salaryMax, salaryCurrency, salaryPeriod, description, requirements, benefits, posted,
                applicationDeadline, applicationsCount, featured, urgent, remote, target.name(), createdAt, createdBy,
                updatedAt, updatedBy);
    }
}

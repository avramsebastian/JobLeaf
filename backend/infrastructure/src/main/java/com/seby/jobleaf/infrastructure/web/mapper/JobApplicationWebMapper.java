package com.seby.jobleaf.infrastructure.web.mapper;

import com.seby.jobleaf.apis.generated.model.ApplicationStatus;
import com.seby.jobleaf.apis.generated.model.JobApplicationResponse;
import com.seby.jobleaf.domain.model.jobapplication.JobApplication;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Maps job applications to the OpenAPI DTOs for the job seeker. The employer's notes are deliberately left out.
 */
@Component
@RequiredArgsConstructor
public class JobApplicationWebMapper {

    private final JobWebMapper jobWebMapper;

    public JobApplicationResponse toJobApplicationResponse(JobApplication application) {
        var response = new JobApplicationResponse();
        response.setId(application.id());
        response.setJob(jobWebMapper.toJobResponse(application.job()));
        response.setAppliedAt(toOffsetDateTime(application.appliedAt()));
        response.setStatus(ApplicationStatus.fromValue(application.status().name()));
        response.setCoverLetter(application.coverLetter());
        response.setUpdatedAt(toOffsetDateTime(application.updatedAt()));
        return response;
    }

    public List<JobApplicationResponse> toJobApplicationResponses(List<JobApplication> applications) {
        return applications.stream().map(this::toJobApplicationResponse).toList();
    }

    private static OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}

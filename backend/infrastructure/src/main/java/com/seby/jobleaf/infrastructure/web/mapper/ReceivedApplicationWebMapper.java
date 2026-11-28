package com.seby.jobleaf.infrastructure.web.mapper;

import com.seby.jobleaf.apis.generated.model.ApplicantResponse;
import com.seby.jobleaf.apis.generated.model.ReceivedApplicationResponse;
import com.seby.jobleaf.apis.generated.model.UpdateJobApplicationRequest;
import com.seby.jobleaf.domain.model.jobapplication.Applicant;
import com.seby.jobleaf.domain.model.jobapplication.ApplicationStatus;
import com.seby.jobleaf.domain.model.jobapplication.JobApplication;
import com.seby.jobleaf.domain.model.jobapplication.ReceivedApplication;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Maps the applications an employer received to the OpenAPI DTOs. Unlike {@link JobApplicationWebMapper} (job
 * seeker), this includes the employer's internal notes and the applicant, but not the whole job (the employer
 * already has it).
 */
@Component
public class ReceivedApplicationWebMapper {

    public ReceivedApplicationResponse toReceivedApplicationResponse(ReceivedApplication received) {
        JobApplication application = received.application();
        var response = new ReceivedApplicationResponse();
        response.setId(application.id());
        response.setJobId(application.job().id());
        response.setApplicant(toApplicantResponse(received.applicant()));
        response.setAppliedAt(toOffsetDateTime(application.appliedAt()));
        response.setStatus(com.seby.jobleaf.apis.generated.model.ApplicationStatus.fromValue(application.status().name()));
        response.setCoverLetter(application.coverLetter());
        response.setNotes(application.notes());
        response.setUpdatedAt(toOffsetDateTime(application.updatedAt()));
        return response;
    }

    public List<ReceivedApplicationResponse> toReceivedApplicationResponses(List<ReceivedApplication> applications) {
        return applications.stream().map(this::toReceivedApplicationResponse).toList();
    }

    public ApplicationStatus toApplicationStatus(UpdateJobApplicationRequest request) {
        return ApplicationStatus.valueOf(request.getStatus().getValue());
    }

    private static ApplicantResponse toApplicantResponse(Applicant applicant) {
        var response = new ApplicantResponse();
        response.setUserId(applicant.userId());
        response.setName(applicant.name());
        response.setEmail(applicant.email());
        response.setMobileNumber(applicant.mobileNumber());
        if (applicant.profile() != null) {
            response.setJobTitle(applicant.profile().jobTitle());
            response.setLocation(applicant.profile().location());
            response.setExperienceLevel(applicant.profile().experienceLevel());
            response.setProfessionalBio(applicant.profile().professionalBio());
            response.setPortfolioWebsite(applicant.profile().portfolioWebsite());
        }
        response.setHasResume(applicant.hasResume());
        response.setHasProfilePicture(applicant.hasProfilePicture());
        return response;
    }

    private static OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}

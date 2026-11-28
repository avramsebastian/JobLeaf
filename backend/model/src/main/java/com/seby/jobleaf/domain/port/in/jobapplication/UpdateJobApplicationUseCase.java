package com.seby.jobleaf.domain.port.in.jobapplication;

import com.seby.jobleaf.domain.model.jobapplication.ApplicationStatus;
import com.seby.jobleaf.domain.model.jobapplication.ReceivedApplication;

import java.util.UUID;

/**
 * Inbound port for an employer changing the status and internal notes of an application they received.
 */
public interface UpdateJobApplicationUseCase {

    /**
     * @param employerId the logged-in user
     * @param notes      null keeps the stored notes, blank text removes them
     * @return the changed application with its applicant
     * @throws com.seby.jobleaf.domain.exceptions.user.EmployerAccessException                  if not an employer or no company
     * @throws com.seby.jobleaf.domain.exceptions.jobapplication.JobApplicationNotFoundException if not for their company's jobs
     * @throws com.seby.jobleaf.domain.exceptions.domain.DomainValidationException              for PENDING / WITHDRAWN or too long notes
     * @throws com.seby.jobleaf.domain.exceptions.jobapplication.JobApplicationConflictException if it was withdrawn
     */
    ReceivedApplication updateJobApplication(UUID employerId, UUID applicationId, ApplicationStatus status,
                                             String notes);
}

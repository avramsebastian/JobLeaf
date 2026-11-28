package com.seby.jobleaf.domain.port.in.jobapplication;

import com.seby.jobleaf.domain.model.profile.ProfileFile;

import java.util.UUID;

/**
 * Inbound port for an employer reading the files of a job seeker who applied to one of their company's jobs.
 * The files are read one at a time, so the list of applications never carries megabytes of file data.
 */
public interface ReadApplicantFilesUseCase {

    /**
     * @param employerId the logged-in user
     * @return the applicant's picture with its content
     * @throws com.seby.jobleaf.domain.exceptions.user.EmployerAccessException                  if not an employer or no company
     * @throws com.seby.jobleaf.domain.exceptions.jobapplication.JobApplicationNotFoundException if not for their company's jobs, or withdrawn
     * @throws com.seby.jobleaf.domain.exceptions.profile.ProfileNotFoundException               if there is no picture
     */
    ProfileFile getApplicantProfilePicture(UUID employerId, UUID applicationId);

    /**
     * Same checks as {@link #getApplicantProfilePicture}.
     *
     * @return the applicant's resume with its content
     */
    ProfileFile getApplicantResume(UUID employerId, UUID applicationId);
}

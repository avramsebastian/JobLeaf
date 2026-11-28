package com.seby.jobleaf.domain.model.jobapplication;

import com.seby.jobleaf.domain.model.profile.ProfileDetails;

import java.util.UUID;

/**
 * The job seeker behind an application, as the employer sees them.
 *
 * @param profile           the profile details, or null if the job seeker has no profile
 * @param hasResume         whether a resume was uploaded (the file itself is not loaded here)
 * @param hasProfilePicture whether a profile picture was uploaded
 */
public record Applicant(
        UUID userId,
        String name,
        String email,
        String mobileNumber,
        ProfileDetails profile,
        boolean hasResume,
        boolean hasProfilePicture
) {
}

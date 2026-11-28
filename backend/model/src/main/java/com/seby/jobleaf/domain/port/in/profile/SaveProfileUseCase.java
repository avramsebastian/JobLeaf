package com.seby.jobleaf.domain.port.in.profile;

import com.seby.jobleaf.domain.model.profile.Profile;
import com.seby.jobleaf.domain.model.profile.ProfileDetails;
import com.seby.jobleaf.domain.model.profile.ProfileFile;

import java.util.UUID;

/**
 * Inbound port for a job seeker creating or updating their profile.
 */
public interface SaveProfileUseCase {

    /**
     * Creates the profile the first time, updates it afterwards. The details are always replaced; a file that is
     * null keeps the stored one (the files are usually added in a second step).
     *
     * @param userId         the logged-in user
     * @param profilePicture an already checked upload ({@link ProfileFile#upload}), or null
     * @param resume         an already checked upload, or null
     * @return the saved profile, without file contents
     * @throws com.seby.jobleaf.domain.exceptions.user.UserNotFoundException      if the user does not exist anymore
     * @throws com.seby.jobleaf.domain.exceptions.user.JobSeekerAccessException  if the user is not a job seeker
     */
    Profile saveProfile(UUID userId, ProfileDetails details, ProfileFile profilePicture, ProfileFile resume);
}

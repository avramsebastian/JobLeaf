package com.seby.jobleaf.domain.port.in.profile;

import com.seby.jobleaf.domain.model.profile.Profile;
import com.seby.jobleaf.domain.model.profile.ProfileFile;

import java.util.UUID;

/**
 * Inbound port for a job seeker reading their profile. The details and the files are read separately, so the
 * details never carry megabytes of file data.
 *
 * @see com.seby.jobleaf.domain.exceptions.profile.ProfileNotFoundException thrown when there is nothing to return
 */
public interface ReadProfileUseCase {

    /**
     * @return the profile without file contents (only names and types)
     */
    Profile getProfile(UUID userId);

    /**
     * @return the picture with its content
     */
    ProfileFile getProfilePicture(UUID userId);

    /**
     * @return the resume with its content
     */
    ProfileFile getResume(UUID userId);
}

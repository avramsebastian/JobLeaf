package com.seby.jobleaf.domain.port.out.profile;

import com.seby.jobleaf.domain.model.profile.Profile;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for storing and reading profiles.
 */
public interface ProfileRepositoryPort {

    /**
     * @param includeBinaryData false: only file names and types (the file contents are not even read from the
     *                          database); true: with the file contents
     */
    Optional<Profile> findByUserId(UUID userId, boolean includeBinaryData);

    /**
     * Creates or updates the profile. Files of the given profile that have no content are kept as stored.
     *
     * @return the saved profile, without file contents
     */
    Profile save(Profile profile);
}

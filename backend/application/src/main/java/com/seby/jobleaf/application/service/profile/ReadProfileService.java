package com.seby.jobleaf.application.service.profile;

import com.seby.jobleaf.domain.exceptions.profile.ProfileNotFoundException;
import com.seby.jobleaf.domain.exceptions.user.UserNotFoundException;
import com.seby.jobleaf.domain.model.profile.Profile;
import com.seby.jobleaf.domain.model.profile.ProfileFile;
import com.seby.jobleaf.domain.port.in.profile.ReadProfileUseCase;
import com.seby.jobleaf.domain.port.out.auth.UserRepositoryPort;
import com.seby.jobleaf.domain.port.out.profile.ProfileRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

/**
 * Application service implementing {@link ReadProfileUseCase}.
 */
@Service
@RequiredArgsConstructor
public class ReadProfileService implements ReadProfileUseCase {

    private static final Logger log = LoggerFactory.getLogger(ReadProfileService.class);

    private final UserRepositoryPort userRepositoryPort;
    private final ProfileRepositoryPort profileRepositoryPort;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Profile getProfile(UUID userId) {
        checkJobSeeker(userId);
        return profileRepositoryPort.findByUserId(userId, false)
                .orElseThrow(ProfileNotFoundException::noProfile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public ProfileFile getProfilePicture(UUID userId) {
        return loadFile(userId, Profile::profilePicture, "profile picture");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public ProfileFile getResume(UUID userId) {
        return loadFile(userId, Profile::resume, "resume");
    }

    private ProfileFile loadFile(UUID userId, Function<Profile, ProfileFile> file, String description) {
        checkJobSeeker(userId);
        var found = profileRepositoryPort.findByUserId(userId, true)
                .map(file)
                .filter(ProfileFile::hasContent)
                .orElseThrow(() -> ProfileNotFoundException.noFile(description));
        log.info("Read {} of user {} ({}, {} bytes)", description, userId, found.type(), found.size());
        return found;
    }

    private void checkJobSeeker(UUID userId) {
        Objects.requireNonNull(userId, "userId");
        userRepositoryPort.findById(userId)
                .orElseThrow(() -> UserNotFoundException.byId(userId))
                .checkIsJobSeeker();
    }
}

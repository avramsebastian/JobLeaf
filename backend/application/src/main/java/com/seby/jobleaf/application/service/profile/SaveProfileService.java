package com.seby.jobleaf.application.service.profile;

import com.seby.jobleaf.domain.exceptions.user.UserNotFoundException;
import com.seby.jobleaf.domain.model.profile.Profile;
import com.seby.jobleaf.domain.model.profile.ProfileDetails;
import com.seby.jobleaf.domain.model.profile.ProfileFile;
import com.seby.jobleaf.domain.port.in.profile.SaveProfileUseCase;
import com.seby.jobleaf.domain.port.out.auth.UserRepositoryPort;
import com.seby.jobleaf.domain.port.out.profile.FileSanitizerPort;
import com.seby.jobleaf.domain.port.out.profile.ProfileRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

/**
 * Application service implementing {@link SaveProfileUseCase}.
 */
@Service
@RequiredArgsConstructor
public class SaveProfileService implements SaveProfileUseCase {

    private static final Logger log = LoggerFactory.getLogger(SaveProfileService.class);

    private final UserRepositoryPort userRepositoryPort;
    private final ProfileRepositoryPort profileRepositoryPort;
    private final FileSanitizerPort fileSanitizerPort;

    /**
     * @return the cleaned file, or null when no file was uploaded (the stored one is kept)
     */
    private ProfileFile sanitize(ProfileFile file) {
        return file == null ? null : fileSanitizerPort.sanitize(file);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Profile saveProfile(UUID userId, ProfileDetails details, ProfileFile profilePicture, ProfileFile resume) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(details, "details");
        userRepositoryPort.findById(userId)
                .orElseThrow(() -> UserNotFoundException.byId(userId))
                .checkIsJobSeeker();

        // Only cleaned files are stored: pictures rebuilt, PDFs with hidden or active content rejected
        ProfileFile cleanPicture = sanitize(profilePicture);
        ProfileFile cleanResume = sanitize(resume);

        var existing = profileRepositoryPort.findByUserId(userId, false);
        var profile = existing.map(stored -> stored.withDetails(details))
                .orElseGet(() -> Profile.create(UUID.randomUUID(), userId, details))
                .withProfilePicture(cleanPicture)
                .withResume(cleanResume);

        var saved = profileRepositoryPort.save(profile);
        log.info("{} profile {} of user {} (new picture: {}, new resume: {})",
                existing.isPresent() ? "Updated" : "Created", saved.id(), userId,
                describe(cleanPicture), describe(cleanResume));
        return saved;
    }

    private static String describe(ProfileFile file) {
        return file == null ? "no" : file.type() + ", " + file.size() + " bytes";
    }
}

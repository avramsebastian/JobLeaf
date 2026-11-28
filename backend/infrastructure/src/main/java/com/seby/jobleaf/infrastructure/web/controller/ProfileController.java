package com.seby.jobleaf.infrastructure.web.controller;

import com.seby.jobleaf.apis.generated.ProfileApi;
import com.seby.jobleaf.apis.generated.model.ProfileRequest;
import com.seby.jobleaf.apis.generated.model.ProfileResponse;
import com.seby.jobleaf.domain.model.profile.ProfileFileKind;
import com.seby.jobleaf.domain.port.in.profile.ReadProfileUseCase;
import com.seby.jobleaf.domain.port.in.profile.SaveProfileUseCase;
import com.seby.jobleaf.infrastructure.web.mapper.ProfileWebMapper;
import com.seby.jobleaf.infrastructure.web.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST adapter implementing generated {@link ProfileApi}: a job seeker's own profile.
 * <p>
 * The security filter chain requires ROLE_JOB_SEEKER ({@code jobSeekerPaths}). The user always comes from the token,
 * so nobody can read or change someone else's profile.
 */
@RestController
@RequiredArgsConstructor
public class ProfileController implements ProfileApi {

    private final SaveProfileUseCase saveProfileUseCase;
    private final ReadProfileUseCase readProfileUseCase;
    private final ProfileWebMapper profileWebMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<ProfileResponse> createOrUpdateProfile(ProfileRequest profile, MultipartFile profilePicture,
                                                                 MultipartFile resume) {
        var saved = saveProfileUseCase.saveProfile(
                AuthenticatedUser.current().id(),
                profileWebMapper.toProfileDetails(profile),
                profileWebMapper.toProfileFile(profilePicture, ProfileFileKind.PROFILE_PICTURE),
                profileWebMapper.toProfileFile(resume, ProfileFileKind.RESUME));
        return ResponseEntity.ok(profileWebMapper.toProfileResponse(saved));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<ProfileResponse> getProfile() {
        var profile = readProfileUseCase.getProfile(AuthenticatedUser.current().id());
        return ResponseEntity.ok(profileWebMapper.toProfileResponse(profile));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<Resource> getProfilePicture() {
        var picture = readProfileUseCase.getProfilePicture(AuthenticatedUser.current().id());
        return profileWebMapper.toFileResponse(picture, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<Resource> getResume() {
        var resume = readProfileUseCase.getResume(AuthenticatedUser.current().id());
        return profileWebMapper.toFileResponse(resume, true);
    }
}

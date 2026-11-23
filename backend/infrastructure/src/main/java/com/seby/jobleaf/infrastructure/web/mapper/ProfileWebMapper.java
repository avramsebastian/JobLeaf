package com.seby.jobleaf.infrastructure.web.mapper;

import com.seby.jobleaf.apis.generated.model.ProfileRequest;
import com.seby.jobleaf.apis.generated.model.ProfileResponse;
import com.seby.jobleaf.domain.model.profile.Profile;
import com.seby.jobleaf.domain.model.profile.ProfileDetails;
import com.seby.jobleaf.domain.model.profile.ProfileFile;
import com.seby.jobleaf.domain.model.profile.ProfileFileKind;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Maps between domain profiles and the OpenAPI profile DTOs, uploads and file responses.
 */
@Component
public class ProfileWebMapper {

    public ProfileDetails toProfileDetails(ProfileRequest request) {
        return new ProfileDetails(
                request.getJobTitle(),
                request.getLocation(),
                request.getExperienceLevel() == null ? null : request.getExperienceLevel().toString(),
                request.getProfessionalBio(),
                request.getPortfolioWebsite()
        );
    }

    /**
     * Turns an uploaded part into a checked domain file ({@link ProfileFile#upload} validates size, type and content).
     *
     * @return null when the part was not sent (or sent empty without a name): the stored file is kept
     */
    public ProfileFile toProfileFile(MultipartFile file, ProfileFileKind kind) {
        if (file == null || (file.isEmpty() && (file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()))) {
            return null;
        }
        try {
            return ProfileFile.upload(kind, file.getOriginalFilename(), file.getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the uploaded " + kind.field(), e);
        }
    }

    /**
     * The profile without file contents: only whether a file exists, its name and type.
     */
    public ProfileResponse toProfileResponse(Profile profile) {
        var details = profile.details();
        var response = new ProfileResponse();
        response.setId(profile.id());
        response.setJobTitle(details.jobTitle());
        response.setLocation(details.location());
        response.setExperienceLevel(details.experienceLevel());
        response.setProfessionalBio(details.professionalBio());
        response.setPortfolioWebsite(details.portfolioWebsite());
        response.setHasProfilePicture(profile.profilePicture() != null);
        response.setHasResume(profile.resume() != null);
        if (profile.profilePicture() != null) {
            response.setProfilePictureName(profile.profilePicture().name());
            response.setProfilePictureType(profile.profilePicture().type().mediaType());
        }
        if (profile.resume() != null) {
            response.setResumeName(profile.resume().name());
            response.setResumeType(profile.resume().type().mediaType());
        }
        response.setCreatedAt(toOffsetDateTime(profile.createdAt()));
        response.setUpdatedAt(toOffsetDateTime(profile.updatedAt()));
        return response;
    }

    /**
     * The file bytes with the detected type (never the type the uploader claimed).
     *
     * @param download true: Content-Disposition "attachment" (the browser downloads it, e.g. the resume);
     *                 false: "inline" (the browser shows it, e.g. the picture in an img tag)
     */
    public ResponseEntity<Resource> toFileResponse(ProfileFile file, boolean download) {
        // ContentDisposition encodes the name (RFC 6266 / 5987), so quotes or other characters can not break the header
        ContentDisposition disposition = (download ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename(file.name(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.type().mediaType()))
                .contentLength(file.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new ByteArrayResource(file.content()));
    }

    private static OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}

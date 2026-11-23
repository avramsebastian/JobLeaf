package com.seby.jobleaf.infrastructure.persistence.mapper;

import com.seby.jobleaf.domain.model.profile.Profile;
import com.seby.jobleaf.domain.model.profile.ProfileDetails;
import com.seby.jobleaf.domain.model.profile.ProfileFile;
import com.seby.jobleaf.domain.model.profile.ProfileFileType;
import com.seby.jobleaf.infrastructure.persistence.entity.JobLeafUserEntity;
import com.seby.jobleaf.infrastructure.persistence.entity.ProfileEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProfilePersistenceMapperTest {

    private final ProfilePersistenceMapper mapper = new ProfilePersistenceMapper();

    private static ProfileEntity entity() {
        var user = new JobLeafUserEntity();
        user.setId(UUID.randomUUID());
        var entity = new ProfileEntity();
        entity.setId(UUID.randomUUID());
        entity.setUser(user);
        entity.setJobTitle("Frontend Developer");
        entity.setLocation("Bucharest");
        entity.setExperienceLevel("Mid Level");
        entity.setProfessionalBio("Frontend developer with React experience.");
        entity.setProfilePicture(new byte[]{1, 2, 3});
        entity.setProfilePictureName("me.png");
        entity.setProfilePictureType("image/png");
        return entity;
    }

    @Test
    @DisplayName("Should leave the file contents out when includeBinaryData is false, and keep name and type")
    void withoutBinaryData() {
        Profile profile = mapper.toDomain(entity(), false);

        assertThat(profile.profilePicture().name()).isEqualTo("me.png");
        assertThat(profile.profilePicture().type()).isEqualTo(ProfileFileType.PNG);
        assertThat(profile.profilePicture().hasContent()).isFalse();
        assertThat(profile.resume()).as("never uploaded").isNull();
    }

    @Test
    @DisplayName("Should include the file contents when includeBinaryData is true")
    void withBinaryData() {
        assertThat(mapper.toDomain(entity(), true).profilePicture().content()).containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("Should copy only files with new content, so a file that was not uploaded stays stored")
    void applyToKeepsStoredFiles() {
        var entity = entity();
        var details = new ProfileDetails("Senior Developer", "Cluj", "Senior Level",
                "Senior developer with React experience.", null);
        var profile = new Profile(entity.getId(), entity.getUser().getId(), details,
                ProfileFile.metadata("me.png", ProfileFileType.PNG),
                new ProfileFile("cv.pdf", ProfileFileType.PDF, new byte[]{'%', 'P', 'D', 'F', '-'}),
                null, null, null, null);

        mapper.applyTo(profile, entity);

        assertThat(entity.getJobTitle()).isEqualTo("Senior Developer");
        assertThat(entity.getProfilePicture()).as("kept").containsExactly(1, 2, 3);
        assertThat(entity.getResumeName()).isEqualTo("cv.pdf");
        assertThat(entity.getResumeType()).isEqualTo("application/pdf");
    }
}

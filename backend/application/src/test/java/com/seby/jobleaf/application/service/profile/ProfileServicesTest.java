package com.seby.jobleaf.application.service.profile;

import com.seby.jobleaf.domain.exceptions.profile.ProfileNotFoundException;
import com.seby.jobleaf.domain.exceptions.user.JobSeekerAccessException;
import com.seby.jobleaf.domain.model.profile.Profile;
import com.seby.jobleaf.domain.model.profile.ProfileDetails;
import com.seby.jobleaf.domain.model.profile.ProfileFile;
import com.seby.jobleaf.domain.model.profile.ProfileFileType;
import com.seby.jobleaf.domain.model.user.JobLeafUser;
import com.seby.jobleaf.domain.model.user.RoleName;
import com.seby.jobleaf.domain.port.out.auth.UserRepositoryPort;
import com.seby.jobleaf.domain.port.out.profile.FileSanitizerPort;
import com.seby.jobleaf.domain.port.out.profile.ProfileRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProfileServicesTest {

    static final UUID USER_ID = UUID.randomUUID();
    static final JobLeafUser SEEKER = new JobLeafUser(USER_ID, "Jane", "jane@example.com", null,
            RoleName.ROLE_JOB_SEEKER, null, null, null);
    static final JobLeafUser EMPLOYER = new JobLeafUser(USER_ID, "Jane", "jane@example.com", null,
            RoleName.ROLE_EMPLOYER, null, null, null);
    static final ProfileDetails DETAILS = new ProfileDetails("Frontend Developer", "Bucharest", "Mid Level",
            "Frontend developer with React experience.", null);

    @Nested
    @ExtendWith(MockitoExtension.class)
    class Save {

        @Mock
        UserRepositoryPort userRepositoryPort;
        @Mock
        ProfileRepositoryPort profileRepositoryPort;
        @Mock
        FileSanitizerPort fileSanitizerPort;
        @InjectMocks
        SaveProfileService service;

        @Test
        @DisplayName("Should create a profile with only the details (step one, no files)")
        void createsWithoutFiles() {
            when(userRepositoryPort.findById(USER_ID)).thenReturn(Optional.of(SEEKER));
            when(profileRepositoryPort.findByUserId(USER_ID, false)).thenReturn(Optional.empty());
            when(profileRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            Profile saved = service.saveProfile(USER_ID, DETAILS, null, null);

            assertThat(saved.userId()).isEqualTo(USER_ID);
            assertThat(saved.profilePicture()).isNull();
            assertThat(saved.resume()).isNull();
        }

        @Test
        @DisplayName("Should update the details and keep the stored picture when no new one is sent")
        void updateKeepsStoredFiles() {
            var storedPicture = ProfileFile.metadata("me.png", ProfileFileType.PNG);
            var stored = Profile.create(UUID.randomUUID(), USER_ID, DETAILS).withProfilePicture(storedPicture);
            var newResume = new ProfileFile("cv.pdf", ProfileFileType.PDF, new byte[]{'%', 'P', 'D', 'F', '-'});
            when(userRepositoryPort.findById(USER_ID)).thenReturn(Optional.of(SEEKER));
            when(profileRepositoryPort.findByUserId(USER_ID, false)).thenReturn(Optional.of(stored));
            when(profileRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            when(fileSanitizerPort.sanitize(newResume)).thenReturn(newResume);

            service.saveProfile(USER_ID, DETAILS, null, newResume);

            var toSave = ArgumentCaptor.forClass(Profile.class);
            verify(profileRepositoryPort).save(toSave.capture());
            assertThat(toSave.getValue().id()).isEqualTo(stored.id());
            assertThat(toSave.getValue().profilePicture()).isSameAs(storedPicture);
            assertThat(toSave.getValue().resume()).isSameAs(newResume);
        }

        @Test
        @DisplayName("Should store the cleaned file returned by the sanitizer, never the upload itself")
        void storesSanitizedFile() {
            var upload = new ProfileFile("me.png", ProfileFileType.PNG, new byte[]{1, 2, 3});
            var cleaned = new ProfileFile("me.jpg", ProfileFileType.JPEG, new byte[]{4, 5});
            when(userRepositoryPort.findById(USER_ID)).thenReturn(Optional.of(SEEKER));
            when(fileSanitizerPort.sanitize(upload)).thenReturn(cleaned);
            when(profileRepositoryPort.findByUserId(USER_ID, false)).thenReturn(Optional.empty());
            when(profileRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            assertThat(service.saveProfile(USER_ID, DETAILS, upload, null).profilePicture()).isSameAs(cleaned);
        }

        @Test
        @DisplayName("Should refuse users who are not job seekers")
        void refusesOtherRoles() {
            when(userRepositoryPort.findById(USER_ID)).thenReturn(Optional.of(EMPLOYER));

            assertThatThrownBy(() -> service.saveProfile(USER_ID, DETAILS, null, null))
                    .isInstanceOf(JobSeekerAccessException.class);
            verifyNoInteractions(profileRepositoryPort);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class Read {

        @Mock
        UserRepositoryPort userRepositoryPort;
        @Mock
        ProfileRepositoryPort profileRepositoryPort;
        @InjectMocks
        ReadProfileService service;

        @Test
        @DisplayName("Should read the details without binary data, and 404 without profile")
        void getProfile() {
            var profile = Profile.create(UUID.randomUUID(), USER_ID, DETAILS);
            when(userRepositoryPort.findById(USER_ID)).thenReturn(Optional.of(SEEKER));
            when(profileRepositoryPort.findByUserId(USER_ID, false)).thenReturn(Optional.of(profile));
            assertThat(service.getProfile(USER_ID)).isSameAs(profile);

            when(profileRepositoryPort.findByUserId(USER_ID, false)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.getProfile(USER_ID)).isInstanceOf(ProfileNotFoundException.class)
                    .hasMessage("You have not created a profile yet");
            verify(profileRepositoryPort, never()).findByUserId(USER_ID, true);
        }

        @Test
        @DisplayName("Should read a file with its content, and 404 when it was never uploaded")
        void getFiles() {
            var picture = new ProfileFile("me.png", ProfileFileType.PNG, new byte[]{1, 2});
            var profile = Profile.create(UUID.randomUUID(), USER_ID, DETAILS).withProfilePicture(picture);
            when(userRepositoryPort.findById(USER_ID)).thenReturn(Optional.of(SEEKER));
            when(profileRepositoryPort.findByUserId(USER_ID, true)).thenReturn(Optional.of(profile));

            assertThat(service.getProfilePicture(USER_ID)).isSameAs(picture);
            assertThatThrownBy(() -> service.getResume(USER_ID)).isInstanceOf(ProfileNotFoundException.class)
                    .hasMessage("You have not uploaded a resume yet");
        }
    }
}

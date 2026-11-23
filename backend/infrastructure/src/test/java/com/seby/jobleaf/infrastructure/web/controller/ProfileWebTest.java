package com.seby.jobleaf.infrastructure.web.controller;

import com.seby.jobleaf.domain.exceptions.profile.ProfileNotFoundException;
import com.seby.jobleaf.domain.model.profile.Profile;
import com.seby.jobleaf.domain.model.profile.ProfileDetails;
import com.seby.jobleaf.domain.model.profile.ProfileFile;
import com.seby.jobleaf.domain.model.profile.ProfileFileType;
import com.seby.jobleaf.domain.port.in.profile.ReadProfileUseCase;
import com.seby.jobleaf.domain.port.in.profile.SaveProfileUseCase;
import com.seby.jobleaf.infrastructure.web.mapper.ProfileWebMapper;
import com.seby.jobleaf.infrastructure.web.security.AccessTokenCookies;
import com.seby.jobleaf.infrastructure.web.security.JobLeafEmailPwdAuthenticationProvider;
import com.seby.jobleaf.infrastructure.web.security.JobLeafSecurityConfig;
import com.seby.jobleaf.infrastructure.web.security.PathsConfig;
import com.seby.jobleaf.infrastructure.web.security.ProblemDetailSecurityHandler;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The profile endpoints through the real security chain, multipart handling and mapper.
 */
@WebMvcTest(ProfileController.class)
@Import({JobLeafSecurityConfig.class, PathsConfig.class, ProblemDetailSecurityHandler.class, AccessTokenCookies.class,
        ProfileWebMapper.class})
class ProfileWebTest {

    private static final String API = "application/vnd.jobleaf+json;v=1.0";
    private static final UUID USER_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final byte[] PDF = {'%', 'P', 'D', 'F', '-', '1', '.', '7'};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1};
    private static final String PROFILE_JSON = """
            {"jobTitle":"Frontend Developer","location":"Bucharest","experienceLevel":"Mid Level",
             "professionalBio":"Frontend developer with React experience."}""";
    private static final ProfileDetails DETAILS = new ProfileDetails("Frontend Developer", "Bucharest", "Mid Level",
            "Frontend developer with React experience.", null);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecretKey jwtSigningKey;

    @MockitoBean
    private JobLeafEmailPwdAuthenticationProvider authenticationProvider;

    @MockitoBean
    private SaveProfileUseCase saveProfileUseCase;

    @MockitoBean
    private ReadProfileUseCase readProfileUseCase;

    private RequestPostProcessor as(String role) {
        String token = Jwts.builder().issuer("jobleaf").subject(USER_ID.toString())
                .claim("email", "jane@example.com").claim("roles", role)
                .expiration(Date.from(Instant.now().plusSeconds(600))).signWith(jwtSigningKey).compact();
        return request -> {
            request.addHeader("Authorization", "Bearer " + token);
            request.setCookies(new Cookie("XSRF-TOKEN", "test-csrf-token"));
            request.addHeader("X-XSRF-TOKEN", "test-csrf-token");
            return request;
        };
    }

    private static MockMultipartFile profilePart(String json) {
        return new MockMultipartFile("profile", "", MediaType.APPLICATION_JSON_VALUE, json.getBytes());
    }

    @Test
    @DisplayName("Should only let job seekers in")
    void otherRoles_rejected() throws Exception {
        mockMvc.perform(get("/jobleaf/profile/jobseeker").accept(API)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/jobleaf/profile/jobseeker").accept(API).with(as("ROLE_EMPLOYER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should save step one (only the JSON part) and answer without file contents")
    void saveDetailsOnly() throws Exception {
        when(saveProfileUseCase.saveProfile(eq(USER_ID), any(), isNull(), isNull()))
                .thenReturn(Profile.create(UUID.randomUUID(), USER_ID, DETAILS));

        mockMvc.perform(multipart(HttpMethod.PUT, "/jobleaf/profile/jobseeker").file(profilePart(PROFILE_JSON))
                        .with(as("ROLE_JOB_SEEKER")).accept(API))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobTitle").value("Frontend Developer"))
                .andExpect(jsonPath("$.hasResume").value(false))
                .andExpect(jsonPath("$.resume").doesNotExist());
    }

    @Test
    @DisplayName("Should pass checked files to the use case (step two)")
    void saveWithFiles() throws Exception {
        when(saveProfileUseCase.saveProfile(eq(USER_ID), any(), any(), any()))
                .thenReturn(Profile.create(UUID.randomUUID(), USER_ID, DETAILS));

        mockMvc.perform(multipart(HttpMethod.PUT, "/jobleaf/profile/jobseeker").file(profilePart(PROFILE_JSON))
                        .file(new MockMultipartFile("resume", "C:\\fakepath\\cv.pdf", "application/pdf", PDF))
                        .file(new MockMultipartFile("profilePicture", "me.png", "image/png", PNG))
                        .with(as("ROLE_JOB_SEEKER")).accept(API))
                .andExpect(status().isOk());

        var resume = ArgumentCaptor.forClass(ProfileFile.class);
        var picture = ArgumentCaptor.forClass(ProfileFile.class);
        verify(saveProfileUseCase).saveProfile(eq(USER_ID), any(), picture.capture(), resume.capture());
        assertThat(resume.getValue().name()).isEqualTo("cv.pdf");
        assertThat(resume.getValue().type()).isEqualTo(ProfileFileType.PDF);
        assertThat(picture.getValue().type()).isEqualTo(ProfileFileType.PNG);
    }

    @Test
    @DisplayName("Should reject a fake file and a missing field with 400, before the use case")
    void invalidRequests_400() throws Exception {
        mockMvc.perform(multipart(HttpMethod.PUT, "/jobleaf/profile/jobseeker").file(profilePart(PROFILE_JSON))
                        .file(new MockMultipartFile("resume", "cv.pdf", "application/pdf", new byte[]{'M', 'Z', 0}))
                        .with(as("ROLE_JOB_SEEKER")).accept(API))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.resume").value("resume content is not a valid pdf file"));

        mockMvc.perform(multipart(HttpMethod.PUT, "/jobleaf/profile/jobseeker")
                        .file(profilePart("{\"jobTitle\":\"Frontend Developer\"}"))
                        .with(as("ROLE_JOB_SEEKER")).accept(API))
                .andExpect(status().isBadRequest());
        verify(saveProfileUseCase, never()).saveProfile(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should send the picture inline and the resume as a download, with the detected type")
    void fileResponses() throws Exception {
        when(readProfileUseCase.getProfilePicture(USER_ID)).thenReturn(new ProfileFile("me.png", ProfileFileType.PNG, PNG));
        when(readProfileUseCase.getResume(USER_ID)).thenReturn(new ProfileFile("My CV.pdf", ProfileFileType.PDF, PDF));

        mockMvc.perform(get("/jobleaf/profile/picture/jobseeker").accept(MediaType.IMAGE_PNG).with(as("ROLE_JOB_SEEKER")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(header().string("Content-Disposition", "inline; filename=\"me.png\"; filename*=UTF-8''me.png"))
                .andExpect(content().bytes(PNG));

        mockMvc.perform(get("/jobleaf/profile/resume/jobseeker").accept(MediaType.APPLICATION_PDF).with(as("ROLE_JOB_SEEKER")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Length", String.valueOf(PDF.length)))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.startsWith("attachment; filename=")));
    }

    @Test
    @DisplayName("Should answer 404 when there is no profile or no file")
    void notFound() throws Exception {
        when(readProfileUseCase.getProfile(USER_ID)).thenThrow(ProfileNotFoundException.noProfile());
        when(readProfileUseCase.getResume(USER_ID)).thenThrow(ProfileNotFoundException.noFile("resume"));

        mockMvc.perform(get("/jobleaf/profile/jobseeker").accept(API).with(as("ROLE_JOB_SEEKER")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/jobleaf/profile/resume/jobseeker").accept(MediaType.APPLICATION_PDF).with(as("ROLE_JOB_SEEKER")))
                .andExpect(status().isNotFound());
    }
}

package com.seby.jobleaf.domain.model.profile;

import com.seby.jobleaf.domain.exceptions.domain.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfileDetailsTest {

    private static ProfileDetails details(String jobTitle, String portfolio) {
        return new ProfileDetails(jobTitle, "Bucharest", "Mid Level", "Frontend developer with React experience.",
                portfolio);
    }

    @Test
    @DisplayName("Should trim the texts and allow an empty portfolio")
    void valid() {
        var details = details("  Frontend Developer ", "  ");

        assertThat(details.jobTitle()).isEqualTo("Frontend Developer");
        assertThat(details.portfolioWebsite()).isNull();
    }

    @Test
    @DisplayName("Should require the four text fields")
    void required() {
        assertThatThrownBy(() -> details(" ", null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("jobTitle is required");
    }

    @Test
    @DisplayName("Should only allow http(s) portfolio links, so a javascript: link can not end up on the page")
    void portfolioMustBeWebLink() {
        assertThat(details("Dev", "https://me.example").portfolioWebsite()).isEqualTo("https://me.example");
        assertThatThrownBy(() -> details("Dev", "javascript:alert(1)"))
                .hasMessage("portfolioWebsite must start with http:// or https://");
    }

    @Test
    @DisplayName("Should keep a stored file when no new one is uploaded")
    void profileKeepsFiles() {
        var picture = ProfileFile.metadata("me.png", ProfileFileType.PNG);
        var profile = Profile.create(UUID.randomUUID(), UUID.randomUUID(), details("Dev", null))
                .withProfilePicture(picture)
                .withResume(null);

        assertThat(profile.profilePicture()).isSameAs(picture);
        assertThat(profile.resume()).isNull();
        assertThat(profile.withDetails(details("Senior Dev", null)).profilePicture()).isSameAs(picture);
    }
}

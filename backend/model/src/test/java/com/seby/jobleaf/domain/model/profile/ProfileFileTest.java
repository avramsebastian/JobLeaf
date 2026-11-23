package com.seby.jobleaf.domain.model.profile;

import com.seby.jobleaf.domain.exceptions.domain.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfileFileTest {

    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};
    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2};
    static final byte[] PDF = {'%', 'P', 'D', 'F', '-', '1', '.', '7', 1, 2};
    /** The start of a Windows program ("MZ"). */
    static final byte[] EXE = {'M', 'Z', (byte) 0x90, 0, 3, 0};

    @Test
    @DisplayName("Should accept png/jpg/jpeg pictures and pdf resumes, with the type detected from the content")
    void validUploads_accepted() {
        assertThat(ProfileFile.upload(ProfileFileKind.PROFILE_PICTURE, "me.png", PNG).type()).isEqualTo(ProfileFileType.PNG);
        assertThat(ProfileFile.upload(ProfileFileKind.PROFILE_PICTURE, "me.JPG", JPEG).type()).isEqualTo(ProfileFileType.JPEG);
        assertThat(ProfileFile.upload(ProfileFileKind.PROFILE_PICTURE, "me.jpeg", JPEG).type()).isEqualTo(ProfileFileType.JPEG);
        var resume = ProfileFile.upload(ProfileFileKind.RESUME, "cv.pdf", PDF);
        assertThat(resume.type().mediaType()).isEqualTo("application/pdf");
        assertThat(resume.size()).isEqualTo(PDF.length);
    }

    @Test
    @DisplayName("Should reject a program renamed to .pdf: the content does not start like a PDF")
    void renamedExecutable_rejected() {
        assertThatThrownBy(() -> ProfileFile.upload(ProfileFileKind.RESUME, "cv.pdf", EXE))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("resume content is not a valid pdf file");
    }

    @Test
    @DisplayName("Should reject extensions that are not allowed for the kind of file")
    void wrongExtension_rejected() {
        assertThatThrownBy(() -> ProfileFile.upload(ProfileFileKind.RESUME, "cv.png", PNG))
                .hasMessage("resume must be one of: pdf");
        assertThatThrownBy(() -> ProfileFile.upload(ProfileFileKind.PROFILE_PICTURE, "me.pdf", PDF))
                .hasMessage("profilePicture must be one of: png, jpg, jpeg");
        assertThatThrownBy(() -> ProfileFile.upload(ProfileFileKind.PROFILE_PICTURE, "me.exe", EXE))
                .hasMessage("profilePicture must be one of: png, jpg, jpeg");
    }

    @Test
    @DisplayName("Should reject a name and content that do not match (a JPEG called .png)")
    void nameAndContentMismatch_rejected() {
        assertThatThrownBy(() -> ProfileFile.upload(ProfileFileKind.PROFILE_PICTURE, "me.png", JPEG))
                .hasMessage("profilePicture content is not a valid png file");
    }

    @Test
    @DisplayName("Should reject empty files and files over 10 MB")
    void sizeLimits() {
        assertThatThrownBy(() -> ProfileFile.upload(ProfileFileKind.RESUME, "cv.pdf", new byte[0]))
                .hasMessage("resume is empty");
        byte[] tooBig = Arrays.copyOf(PDF, ProfileFile.MAX_SIZE_BYTES + 1);
        assertThatThrownBy(() -> ProfileFile.upload(ProfileFileKind.RESUME, "cv.pdf", tooBig))
                .hasMessage("resume must be at most 10 MB");
        byte[] exactly10Mb = Arrays.copyOf(PDF, ProfileFile.MAX_SIZE_BYTES);
        assertThat(ProfileFile.upload(ProfileFileKind.RESUME, "cv.pdf", exactly10Mb).size())
                .isEqualTo(ProfileFile.MAX_SIZE_BYTES);
    }

    @Test
    @DisplayName("Should clean file names: no paths, no quotes or control characters")
    void safeName() {
        assertThat(ProfileFile.safeName("C:\\fakepath\\My CV.pdf")).isEqualTo("My CV.pdf");
        assertThat(ProfileFile.safeName("../../etc/cv.pdf")).isEqualTo("cv.pdf");
        assertThat(ProfileFile.safeName("cv\";\r\nSet-Cookie: x.pdf")).isEqualTo("cvSet-Cookie x.pdf");
        assertThat(ProfileFile.safeName("a".repeat(300) + ".pdf")).hasSize(255).endsWith(".pdf");
    }
}

package com.seby.jobleaf.domain.model.profile;

import com.seby.jobleaf.domain.exceptions.domain.DomainValidationException;

import java.util.Locale;

/**
 * One file of a profile: its (cleaned) name, its detected type and, when loaded, its content.
 * <p>
 * {@link #upload} is the only way to create a file from user input, and it checks everything: not empty, at most
 * {@value #MAX_SIZE_BYTES} bytes, an allowed extension for this kind of file, and content that really is that format
 * (magic bytes). {@link #metadata} is for files read back without their bytes ({@code content} is then null).
 */
public record ProfileFile(String name, ProfileFileType type, byte[] content) {

    /** 10 MB. */
    public static final int MAX_SIZE_BYTES = 10 * 1024 * 1024;
    static final int MAX_NAME_LENGTH = 255;

    /**
     * Checks an uploaded file and returns it with a safe name and the detected type.
     *
     * @throws DomainValidationException (400) naming the request part, e.g. "resume"
     */
    public static ProfileFile upload(ProfileFileKind kind, String originalName, byte[] content) {
        String field = kind.field();
        if (content == null || content.length == 0) {
            throw DomainValidationException.invalidField(field, field + " is empty");
        }
        if (content.length > MAX_SIZE_BYTES) {
            throw DomainValidationException.invalidField(field, field + " must be at most 10 MB");
        }

        String name = safeName(originalName);
        String extension = extension(name);
        if (!kind.allowsExtension(extension)) {
            throw DomainValidationException.invalidField(field,
                    field + " must be one of: " + kind.allowedExtensions());
        }

        // The name says what the file claims to be, the first bytes say what it is: both must agree
        ProfileFileType detected = ProfileFileType.detect(content)
                .filter(kind.allowedTypes()::contains)
                .filter(type -> type.extensions().contains(extension))
                .orElseThrow(() -> DomainValidationException.invalidField(field,
                        field + " content is not a valid " + extension + " file"));
        return new ProfileFile(name, detected, content);
    }

    /**
     * A stored file without its bytes, e.g. for the profile details.
     */
    public static ProfileFile metadata(String name, ProfileFileType type) {
        return new ProfileFile(name, type, null);
    }

    public boolean hasContent() {
        return content != null;
    }

    public int size() {
        return content == null ? 0 : content.length;
    }

    /**
     * Keeps only the last path part ("C:\fakepath\cv.pdf" or "../../cv.pdf" become "cv.pdf") and removes characters
     * that could break the Content-Disposition header or the file system: control characters, quotes, slashes, ";".
     */
    static String safeName(String originalName) {
        String name = originalName == null ? "" : originalName;
        name = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
        name = name.replaceAll("[\\p{Cntrl}\"';<>|*?:]", "").replaceAll("\\s+", " ").trim();
        if (name.length() > MAX_NAME_LENGTH) {
            String extension = extension(name);
            name = name.substring(0, MAX_NAME_LENGTH - extension.length() - 1) + "." + extension;
        }
        return name;
    }

    private static String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}

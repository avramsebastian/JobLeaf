package com.seby.jobleaf.domain.model.profile;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The two files of a profile and which formats each one accepts.
 */
public enum ProfileFileKind {

    PROFILE_PICTURE("profilePicture", List.of(ProfileFileType.PNG, ProfileFileType.JPEG)),
    RESUME("resume", List.of(ProfileFileType.PDF));

    private final String field;
    private final List<ProfileFileType> allowedTypes;

    ProfileFileKind(String field, List<ProfileFileType> allowedTypes) {
        this.field = field;
        this.allowedTypes = allowedTypes;
    }

    /**
     * @return the request part name, used as the field name in validation errors
     */
    public String field() {
        return field;
    }

    public List<ProfileFileType> allowedTypes() {
        return allowedTypes;
    }

    public boolean allowsExtension(String extension) {
        return allowedTypes.stream().anyMatch(type -> type.extensions().contains(extension));
    }

    /**
     * @return e.g. "png, jpg, jpeg" for error messages
     */
    public String allowedExtensions() {
        return allowedTypes.stream().flatMap(type -> type.extensions().stream()).collect(Collectors.joining(", "));
    }
}

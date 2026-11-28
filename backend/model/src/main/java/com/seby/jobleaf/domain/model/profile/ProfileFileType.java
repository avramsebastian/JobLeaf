package com.seby.jobleaf.domain.model.profile;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * The file formats a profile accepts, each recognised by its "magic bytes": the fixed signature every file of that
 * format starts with. The file name and the Content-Type the browser sends can be faked; the content can not, so the
 * type stored and sent back is always the one detected here.
 */
public enum ProfileFileType {

    PNG("image/png", List.of("png"), new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A}),
    JPEG("image/jpeg", List.of("jpg", "jpeg"), new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
    PDF("application/pdf", List.of("pdf"), new byte[]{'%', 'P', 'D', 'F', '-'});

    private final String mediaType;
    private final List<String> extensions;
    private final byte[] signature;

    ProfileFileType(String mediaType, List<String> extensions, byte[] signature) {
        this.mediaType = mediaType;
        this.extensions = extensions;
        this.signature = signature;
    }

    public String mediaType() {
        return mediaType;
    }

    public List<String> extensions() {
        return extensions;
    }

    /**
     * @return the type whose signature the content starts with, if any
     */
    public static Optional<ProfileFileType> detect(byte[] content) {
        return Arrays.stream(values()).filter(type -> type.matches(content)).findFirst();
    }

    /**
     * @return the type stored under this media type, e.g. "image/png" (used when reading files back)
     */
    public static Optional<ProfileFileType> fromMediaType(String mediaType) {
        return Arrays.stream(values()).filter(type -> type.mediaType.equals(mediaType)).findFirst();
    }

    private boolean matches(byte[] content) {
        if (content == null || content.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (content[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }
}

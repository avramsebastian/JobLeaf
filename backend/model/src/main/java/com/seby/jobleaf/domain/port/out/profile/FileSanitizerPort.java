package com.seby.jobleaf.domain.port.out.profile;

import com.seby.jobleaf.domain.model.profile.ProfileFile;

/**
 * Outbound port that cleans an uploaded file before it is stored, against content hidden inside a file that is
 * otherwise valid (steganography, appended data, metadata, active PDF content).
 * <p>
 * Called after {@link ProfileFile#upload} has checked size, extension and format.
 */
public interface FileSanitizerPort {

    /**
     * @return the file to store: a rebuilt copy for pictures, the same file for a clean PDF
     * @throws com.seby.jobleaf.domain.exceptions.domain.DomainValidationException (400) if the file can not be made
     *                                                                             safe
     */
    ProfileFile sanitize(ProfileFile file);
}

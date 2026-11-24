package com.seby.jobleaf.domain.model.jobapplication;

import com.seby.jobleaf.domain.exceptions.domain.DomainValidationException;
import com.seby.jobleaf.domain.exceptions.jobapplication.JobApplicationConflictException;
import com.seby.jobleaf.domain.model.job.Job;

import java.time.Instant;
import java.util.UUID;

import static com.seby.jobleaf.domain.util.TextUtility.normalizeNullableText;

/**
 * One job seeker's application to one job.
 *
 * @param notes internal notes of the employer; never shown to the job seeker
 */
public record JobApplication(
        UUID id,
        UUID userId,
        Job job,
        Instant appliedAt,
        ApplicationStatus status,
        String coverLetter,
        String notes,
        Instant createdAt,
        UUID createdBy,
        Instant updatedAt,
        UUID updatedBy
) {

    public static final int MAX_COVER_LETTER_LENGTH = 5000;

    /**
     * A new application in status PENDING.
     */
    public static JobApplication create(UUID id, UUID userId, Job job, String coverLetter, Instant now) {
        return new JobApplication(id, userId, job, now, ApplicationStatus.PENDING, checkedCoverLetter(coverLetter),
                null, null, userId, null, userId);
    }

    /**
     * Withdrawn applications do not count: the job seeker can apply again.
     */
    public boolean isWithdrawn() {
        return status == ApplicationStatus.WITHDRAWN;
    }

    /**
     * Applying again after withdrawing reopens the same application (one per user and job), with the new cover letter.
     */
    public JobApplication reapply(String newCoverLetter, Instant now) {
        return new JobApplication(id, userId, job, now, ApplicationStatus.PENDING, checkedCoverLetter(newCoverLetter),
                notes, createdAt, createdBy, updatedAt, updatedBy);
    }

    /**
     * @throws JobApplicationConflictException if the employer already rejected it: there is nothing to withdraw
     */
    public JobApplication withdraw() {
        if (status == ApplicationStatus.REJECTED) {
            throw JobApplicationConflictException.alreadyRejected(job.id());
        }
        return new JobApplication(id, userId, job, appliedAt, ApplicationStatus.WITHDRAWN, coverLetter, notes,
                createdAt, createdBy, updatedAt, updatedBy);
    }

    /**
     * @return the trimmed cover letter, or null when empty
     */
    private static String checkedCoverLetter(String coverLetter) {
        String normalized = normalizeNullableText(coverLetter);
        if (normalized != null && normalized.length() > MAX_COVER_LETTER_LENGTH) {
            throw DomainValidationException.invalidField("coverLetter",
                    "coverLetter must be at most " + MAX_COVER_LETTER_LENGTH + " characters");
        }
        return normalized;
    }
}

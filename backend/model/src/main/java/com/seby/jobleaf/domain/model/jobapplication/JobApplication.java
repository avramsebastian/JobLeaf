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

    /**
     * Longest cover letter and longest notes.
     */
    public static final int MAX_TEXT_LENGTH = 5000;

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
     * The employer moves the application forward (or rejects it) and keeps internal notes.
     *
     * @param newStatus one of {@link ApplicationStatus#isSetByEmployer() the employer's statuses}
     * @param newNotes  null keeps the stored notes, blank text removes them
     * @throws DomainValidationException       for PENDING / WITHDRAWN, or notes over 5000 characters
     * @throws JobApplicationConflictException if the job seeker withdrew it
     */
    public JobApplication review(ApplicationStatus newStatus, String newNotes) {
        if (newStatus == null || !newStatus.isSetByEmployer()) {
            throw DomainValidationException.invalidField("status",
                    "status must be one of REVIEWED, SHORTLISTED, INTERVIEWED, OFFERED, REJECTED");
        }
        if (isWithdrawn()) {
            throw JobApplicationConflictException.withdrawn(id);
        }
        String notesToKeep = newNotes == null ? notes : checkedLength(newNotes, "notes");
        return new JobApplication(id, userId, job, appliedAt, newStatus, coverLetter, notesToKeep,
                createdAt, createdBy, updatedAt, updatedBy);
    }

    /**
     * @return the trimmed cover letter, or null when empty
     */
    private static String checkedCoverLetter(String coverLetter) {
        return checkedLength(coverLetter, "coverLetter");
    }

    /**
     * @return the trimmed text, or null when empty
     */
    private static String checkedLength(String text, String field) {
        String normalized = normalizeNullableText(text);
        if (normalized != null && normalized.length() > MAX_TEXT_LENGTH) {
            throw DomainValidationException.invalidField(field,
                    field + " must be at most " + MAX_TEXT_LENGTH + " characters");
        }
        return normalized;
    }
}

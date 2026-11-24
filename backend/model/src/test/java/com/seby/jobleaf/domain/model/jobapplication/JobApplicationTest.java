package com.seby.jobleaf.domain.model.jobapplication;

import com.seby.jobleaf.domain.exceptions.domain.DomainValidationException;
import com.seby.jobleaf.domain.exceptions.jobapplication.JobApplicationConflictException;
import com.seby.jobleaf.domain.model.job.Job;
import com.seby.jobleaf.domain.model.job.JobDetails;
import com.seby.jobleaf.domain.model.job.JobStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JobApplicationTest {

    static final Instant NOW = Instant.parse("2026-11-24T10:00:00Z");

    static Job job() {
        var details = new JobDetails("Frontend Developer", "Bucharest", "Hybrid", "Full-time", "Technology",
                "Mid Level", BigDecimal.ONE, BigDecimal.TEN, "EUR", "year", "We are looking for a developer.",
                List.of("React"), List.of("Remote Work"), Instant.now().plus(Duration.ofDays(30)), false, false, false);
        return Job.create(UUID.randomUUID(), UUID.randomUUID(), details, NOW, UUID.randomUUID())
                .withStatus(JobStatus.ACTIVE, NOW);
    }

    private static JobApplication application(ApplicationStatus status) {
        var created = JobApplication.create(UUID.randomUUID(), UUID.randomUUID(), job(), "Hello", NOW);
        return new JobApplication(created.id(), created.userId(), created.job(), created.appliedAt(), status,
                created.coverLetter(), "internal note", NOW, created.userId(), NOW, created.userId());
    }

    @Test
    @DisplayName("Should create a PENDING application with a trimmed cover letter, or none when blank")
    void create() {
        var application = JobApplication.create(UUID.randomUUID(), UUID.randomUUID(), job(), "  Hello  ", NOW);
        assertThat(application.status()).isEqualTo(ApplicationStatus.PENDING);
        assertThat(application.appliedAt()).isEqualTo(NOW);
        assertThat(application.coverLetter()).isEqualTo("Hello");
        assertThat(JobApplication.create(UUID.randomUUID(), UUID.randomUUID(), job(), "  ", NOW).coverLetter()).isNull();
    }

    @Test
    @DisplayName("Should reject cover letters over 5000 characters")
    void coverLetterTooLong() {
        assertThatThrownBy(() -> JobApplication.create(UUID.randomUUID(), UUID.randomUUID(), job(), "a".repeat(5001), NOW))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("coverLetter must be at most 5000 characters");
    }

    @Test
    @DisplayName("Should withdraw an application, but not a rejected one")
    void withdraw() {
        assertThat(application(ApplicationStatus.REVIEWED).withdraw().status()).isEqualTo(ApplicationStatus.WITHDRAWN);
        assertThatThrownBy(() -> application(ApplicationStatus.REJECTED).withdraw())
                .isInstanceOf(JobApplicationConflictException.class);
    }

    @Test
    @DisplayName("Should reopen a withdrawn application as PENDING with a new date and cover letter, keeping the id")
    void reapply() {
        var withdrawn = application(ApplicationStatus.WITHDRAWN);
        Instant later = NOW.plus(Duration.ofDays(2));

        var reopened = withdrawn.reapply("New letter", later);

        assertThat(reopened.id()).isEqualTo(withdrawn.id());
        assertThat(reopened.status()).isEqualTo(ApplicationStatus.PENDING);
        assertThat(reopened.appliedAt()).isEqualTo(later);
        assertThat(reopened.coverLetter()).isEqualTo("New letter");
        assertThat(reopened.notes()).as("the employer's notes stay").isEqualTo("internal note");
    }
}

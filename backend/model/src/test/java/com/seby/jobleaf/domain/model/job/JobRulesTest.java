package com.seby.jobleaf.domain.model.job;

import com.seby.jobleaf.domain.model.user.JobLeafUser;
import com.seby.jobleaf.domain.model.user.RoleName;
import com.seby.jobleaf.domain.model.user.SystemActor;

import com.seby.jobleaf.domain.exceptions.domain.DomainValidationException;
import com.seby.jobleaf.domain.exceptions.job.InvalidJobStatusChangeException;
import com.seby.jobleaf.domain.exceptions.user.EmployerAccessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JobRulesTest {

    static JobDetails details(BigDecimal min, BigDecimal max, Instant deadline, List<String> requirements) {
        return new JobDetails(" Frontend Developer ", "Bucharest", "Hybrid", "Full-time", "Technology", "Mid Level",
                min, max, "EUR", "year", "We are looking for a frontend developer.", requirements,
                List.of("Remote Work"), deadline, false, true, false);
    }

    static JobDetails validDetails() {
        return details(BigDecimal.valueOf(50_000), BigDecimal.valueOf(80_000),
                Instant.now().plus(Duration.ofDays(30)), List.of("React"));
    }

    @Test
    @DisplayName("Should normalize valid details: trimmed texts, blank list items dropped")
    void validDetails_normalized() {
        var details = details(BigDecimal.ONE, BigDecimal.ONE, Instant.now().plus(Duration.ofDays(1)),
                Arrays.asList(" React ", " ", null));

        assertThat(details.title()).isEqualTo("Frontend Developer");
        assertThat(details.requirements()).containsExactly("React");
    }

    @Test
    @DisplayName("Should reject a salary range that is upside down, a past deadline and empty lists")
    void invalidDetails_rejected() {
        Instant future = Instant.now().plus(Duration.ofDays(1));
        assertThatThrownBy(() -> details(BigDecimal.TEN, BigDecimal.ONE, future, List.of("React")))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("salaryMax must be greater than or equal to salaryMin");
        assertThatThrownBy(() -> details(BigDecimal.ONE, BigDecimal.TEN, Instant.now().minusSeconds(60), List.of("React")))
                .hasMessage("applicationDeadline must be in the future");
        assertThatThrownBy(() -> details(BigDecimal.ONE, BigDecimal.TEN, future, List.of(" ")))
                .hasMessage("requirements needs at least one item");
        assertThatThrownBy(() -> details(BigDecimal.valueOf(-1), BigDecimal.TEN, future, List.of("React")))
                .hasMessage("salaryMin can not be negative");
    }

    @Test
    @DisplayName("Should create jobs as DRAFT with no applications")
    void create_isDraft() {
        Instant now = Instant.now();
        var job = Job.create(UUID.randomUUID(), UUID.randomUUID(), validDetails(), now, SystemActor.ID);

        assertThat(job.currentStatus()).isEqualTo(JobStatus.DRAFT);
        assertThat(job.applicationsCount()).isZero();
        assertThat(job.postedDate()).isEqualTo(now);
    }

    @Test
    @DisplayName("Should set postedDate when a draft is published, and keep it on later changes")
    void withStatus_publishSetsPostedDate() {
        Instant created = Instant.parse("2026-11-01T10:00:00Z");
        Instant published = Instant.parse("2026-11-05T10:00:00Z");
        var draft = Job.create(UUID.randomUUID(), UUID.randomUUID(), validDetails(), created, SystemActor.ID);

        var active = draft.withStatus(JobStatus.ACTIVE, published);
        var closed = active.withStatus(JobStatus.CLOSED, Instant.parse("2026-11-09T10:00:00Z"));

        assertThat(active.postedDate()).isEqualTo(published);
        assertThat(closed.postedDate()).isEqualTo(published);
        assertThat(closed.currentStatus()).isEqualTo(JobStatus.CLOSED);
    }

    @Test
    @DisplayName("Should never let a published or closed job go back to DRAFT")
    void backToDraft_forbidden() {
        assertThatThrownBy(() -> JobStatus.ACTIVE.checkCanChangeTo(JobStatus.DRAFT))
                .isInstanceOf(InvalidJobStatusChangeException.class)
                .hasMessage("A job can not change from ACTIVE to DRAFT");
        assertThatThrownBy(() -> JobStatus.CLOSED.checkCanChangeTo(JobStatus.DRAFT))
                .isInstanceOf(InvalidJobStatusChangeException.class);
        assertThatCode(() -> JobStatus.CLOSED.checkCanChangeTo(JobStatus.ACTIVE)).doesNotThrowAnyException();
        assertThatCode(() -> JobStatus.DRAFT.checkCanChangeTo(JobStatus.CLOSED)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should only give the company of an employer that has one")
    void requireEmployerCompany() {
        UUID companyId = UUID.randomUUID();
        var employer = new JobLeafUser(UUID.randomUUID(), "E", "e@example.com", null, RoleName.ROLE_EMPLOYER,
                companyId, "DerpCorp", null);
        var noCompany = new JobLeafUser(UUID.randomUUID(), "E", "e@example.com", null, RoleName.ROLE_EMPLOYER,
                null, null, null);
        var seeker = new JobLeafUser(UUID.randomUUID(), "S", "s@example.com", null, RoleName.ROLE_JOB_SEEKER,
                null, null, null);

        assertThat(employer.requireEmployerCompany()).isEqualTo(companyId);
        assertThatThrownBy(noCompany::requireEmployerCompany).isInstanceOf(EmployerAccessException.class)
                .hasMessageContaining("not assigned to a company");
        assertThatThrownBy(seeker::requireEmployerCompany).isInstanceOf(EmployerAccessException.class)
                .hasMessageContaining("not an employer");
    }
}

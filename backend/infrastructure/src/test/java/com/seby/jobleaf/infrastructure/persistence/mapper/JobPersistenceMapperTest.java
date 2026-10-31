package com.seby.jobleaf.infrastructure.persistence.mapper;

import com.seby.jobleaf.domain.model.Job;
import com.seby.jobleaf.infrastructure.persistence.entity.CompanyEntity;
import com.seby.jobleaf.infrastructure.persistence.entity.JobEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JobPersistenceMapperTest {

    private JobPersistenceMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new JobPersistenceMapper();
    }

    @Test
    @DisplayName("Should map JobEntity to domain Job with all properties and the owning company id")
    void toDomain_mapsAllFieldsCorrectly() {
        // Given
        UUID id = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        Instant postedDate = Instant.parse("2025-10-05T19:27:59Z");
        Instant deadline = Instant.parse("2025-12-21T11:27:59Z");
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");

        CompanyEntity company = new CompanyEntity();
        company.setId(companyId);

        JobEntity entity = new JobEntity();
        entity.setId(id);
        entity.setCompany(company);
        entity.setTitle("Design System Lead");
        entity.setLocation("Remote");
        entity.setWorkType("Hybrid");
        entity.setJobType("Full-time");
        entity.setCategory("Design");
        entity.setExperienceLevel("Entry Level");
        entity.setSalaryMin(new BigDecimal("52000.00"));
        entity.setSalaryMax(new BigDecimal("88000.00"));
        entity.setSalaryCurrency("USD");
        entity.setSalaryPeriod("year");
        entity.setDescription("Join our design team.");
        entity.setRequirements(List.of("Figma", "Design Systems"));
        entity.setBenefits(List.of("Paid Time Off"));
        entity.setPostedDate(postedDate);
        entity.setApplicationDeadline(deadline);
        entity.setApplicationsCount(143);
        entity.setFeatured(true);
        entity.setUrgent(false);
        entity.setRemote(true);
        entity.setStatus("ACTIVE");
        entity.restoreAuditFields(createdAt, createdBy, null, null);

        // When
        Job domain = mapper.toDomain(entity);

        // Then
        assertThat(domain).isNotNull();
        assertThat(domain.id()).isEqualTo(id);
        assertThat(domain.companyId()).isEqualTo(companyId);
        assertThat(domain.title()).isEqualTo("Design System Lead");
        assertThat(domain.location()).isEqualTo("Remote");
        assertThat(domain.workType()).isEqualTo("Hybrid");
        assertThat(domain.jobType()).isEqualTo("Full-time");
        assertThat(domain.category()).isEqualTo("Design");
        assertThat(domain.experienceLevel()).isEqualTo("Entry Level");
        assertThat(domain.salaryMin()).isEqualByComparingTo("52000");
        assertThat(domain.salaryMax()).isEqualByComparingTo("88000");
        assertThat(domain.salaryCurrency()).isEqualTo("USD");
        assertThat(domain.salaryPeriod()).isEqualTo("year");
        assertThat(domain.description()).isEqualTo("Join our design team.");
        assertThat(domain.requirements()).containsExactly("Figma", "Design Systems");
        assertThat(domain.benefits()).containsExactly("Paid Time Off");
        assertThat(domain.postedDate()).isEqualTo(postedDate);
        assertThat(domain.applicationDeadline()).isEqualTo(deadline);
        assertThat(domain.applicationsCount()).isEqualTo(143);
        assertThat(domain.featured()).isTrue();
        assertThat(domain.urgent()).isFalse();
        assertThat(domain.remote()).isTrue();
        assertThat(domain.status()).isEqualTo("ACTIVE");
        assertThat(domain.createdAt()).isEqualTo(createdAt);
        assertThat(domain.createdBy()).isEqualTo(createdBy);
    }

    @Test
    @DisplayName("Should return null when mapping null JobEntity")
    void toDomain_whenEntityIsNull_returnsNull() {
        assertThat(mapper.toDomain(null)).isNull();
    }

    @Test
    @DisplayName("Should map domain Job to JobEntity and set the company back-reference")
    void toEntity_mapsFieldsAndSetsCompany() {
        // Given
        CompanyEntity company = new CompanyEntity();
        company.setId(UUID.randomUUID());
        Job job = new Job(
                UUID.randomUUID(), company.getId(), "Product Designer", "Nashville, TN", "On-site", "Part-time",
                "Design", "Executive Level", new BigDecimal("181000"), new BigDecimal("310000"), "USD", "year",
                "Design things.", List.of("Figma"), List.of("401(k)"), Instant.now(), null, 290,
                false, false, false, "ACTIVE", Instant.now(), UUID.randomUUID(), null, null
        );

        // When
        JobEntity entity = mapper.toEntity(job, company);

        // Then
        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isEqualTo(job.id());
        assertThat(entity.getCompany()).isSameAs(company);
        assertThat(entity.getTitle()).isEqualTo("Product Designer");
        assertThat(entity.getRequirements()).containsExactly("Figma");
        assertThat(entity.getApplicationsCount()).isEqualTo(290);
        assertThat(entity.getCreatedBy()).isEqualTo(job.createdBy());
    }
}

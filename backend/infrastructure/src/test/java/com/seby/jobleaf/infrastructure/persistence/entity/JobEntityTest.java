package com.seby.jobleaf.infrastructure.persistence.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JobEntityTest {

    @Test
    @DisplayName("Should correctly set and get all fields via setters and getters")
    void shouldSetAndGetAllFields() {
        // Given
        JobEntity entity = new JobEntity();
        CompanyEntity company = new CompanyEntity();
        UUID id = UUID.randomUUID();
        Instant postedDate = Instant.parse("2025-10-05T19:27:59Z");
        Instant deadline = Instant.parse("2025-12-21T11:27:59Z");

        // When
        entity.id = id;
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
        entity.setRequirements(List.of("Figma"));
        entity.setBenefits(List.of("Paid Time Off"));
        entity.setPostedDate(postedDate);
        entity.setApplicationDeadline(deadline);
        entity.setApplicationsCount(143);
        entity.setFeatured(true);
        entity.setUrgent(true);
        entity.setRemote(false);
        entity.setStatus("ACTIVE");

        // Then
        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getCompany()).isSameAs(company);
        assertThat(entity.getTitle()).isEqualTo("Design System Lead");
        assertThat(entity.getLocation()).isEqualTo("Remote");
        assertThat(entity.getWorkType()).isEqualTo("Hybrid");
        assertThat(entity.getJobType()).isEqualTo("Full-time");
        assertThat(entity.getCategory()).isEqualTo("Design");
        assertThat(entity.getExperienceLevel()).isEqualTo("Entry Level");
        assertThat(entity.getSalaryMin()).isEqualByComparingTo("52000");
        assertThat(entity.getSalaryMax()).isEqualByComparingTo("88000");
        assertThat(entity.getSalaryCurrency()).isEqualTo("USD");
        assertThat(entity.getSalaryPeriod()).isEqualTo("year");
        assertThat(entity.getDescription()).isEqualTo("Join our design team.");
        assertThat(entity.getRequirements()).containsExactly("Figma");
        assertThat(entity.getBenefits()).containsExactly("Paid Time Off");
        assertThat(entity.getPostedDate()).isEqualTo(postedDate);
        assertThat(entity.getApplicationDeadline()).isEqualTo(deadline);
        assertThat(entity.getApplicationsCount()).isEqualTo(143);
        assertThat(entity.isFeatured()).isTrue();
        assertThat(entity.isUrgent()).isTrue();
        assertThat(entity.isRemote()).isFalse();
        assertThat(entity.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("Should default boolean flags to false")
    void shouldDefaultFlagsToFalse() {
        JobEntity entity = new JobEntity();

        assertThat(entity.isFeatured()).isFalse();
        assertThat(entity.isUrgent()).isFalse();
        assertThat(entity.isRemote()).isFalse();
    }
}

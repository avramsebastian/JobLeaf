package com.seby.jobleaf.infrastructure.web.mapper;

import com.seby.jobleaf.apis.generated.model.JobResponse;
import com.seby.jobleaf.domain.model.Job;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JobWebMapperTest {

    private JobWebMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new JobWebMapper();
    }

    @Test
    @DisplayName("Should map domain Job to JobResponse with UTC OffsetDateTime")
    void toJobResponse_mapsAllFieldsCorrectly() {
        // Given
        Instant postedDate = Instant.parse("2025-10-05T19:27:59Z");
        Job job = new Job(
                UUID.randomUUID(), UUID.randomUUID(), "Operations Director", "Remote", "Hybrid", "Freelance",
                "Operations", "Mid Level", new BigDecimal("62000"), new BigDecimal("107000"), "USD", "year",
                "Run operations.", List.of("Six Sigma"), List.of("Vision Insurance"), postedDate, null, 491,
                false, false, true, "ACTIVE", null, UUID.randomUUID(), null, null
        );

        // When
        JobResponse response = mapper.toJobResponse(job);

        // Then
        assertThat(response.getId()).isEqualTo(job.id());
        assertThat(response.getCompanyId()).isEqualTo(job.companyId());
        assertThat(response.getTitle()).isEqualTo("Operations Director");
        assertThat(response.getSalaryMin()).isEqualByComparingTo("62000");
        assertThat(response.getSalaryMax()).isEqualByComparingTo("107000");
        assertThat(response.getRequirements()).containsExactly("Six Sigma");
        assertThat(response.getBenefits()).containsExactly("Vision Insurance");
        assertThat(response.getPostedDate()).isEqualTo(postedDate.atOffset(ZoneOffset.UTC));
        assertThat(response.getApplicationDeadline()).isNull();
        assertThat(response.getApplicationsCount()).isEqualTo(491);
        assertThat(response.getRemote()).isTrue();
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getCreatedBy()).isEqualTo(job.createdBy());
    }

    @Test
    @DisplayName("Should return null when the jobs list is null")
    void toListJobsResponse_whenNull_returnsNull() {
        assertThat(mapper.toListJobsResponse(null)).isNull();
    }
}

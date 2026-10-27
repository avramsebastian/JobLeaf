package com.seby.jobleaf.infrastructure.web.mapper;

import com.seby.jobleaf.apis.generated.model.CompanyResponse;
import com.seby.jobleaf.domain.model.Company;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyWebMapperTest {

    private CompanyWebMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new CompanyWebMapper();
    }

    @Test
    @DisplayName("Should map domain Company to CompanyResponse with UTC OffsetDateTime")
    void toCompanyResponse_mapsAllFieldsCorrectly() {
        // Given
        UUID id = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        UUID updatedBy = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-03-15T08:30:00Z");
        Instant updatedAt = Instant.parse("2026-03-16T14:45:00Z");

        Company company = new Company(
                id,
                "JobLeaf Global",
                "/logos/jobleaf.png",
                "Human Resources",
                "Enterprise",
                4.95,
                "London, Berlin, New York",
                2019,
                "Modern recruitment platform.",
                800,
                "https://jobleaf.example",
                createdAt,
                createdBy,
                updatedAt,
                updatedBy
        );

        // When
        CompanyResponse response = mapper.toCompanyResponse(company);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getName()).isEqualTo("JobLeaf Global");
        assertThat(response.getLogo()).isEqualTo("/logos/jobleaf.png");
        assertThat(response.getIndustry()).isEqualTo("Human Resources");
        assertThat(response.getSize()).isEqualTo("Enterprise");
        assertThat(response.getRating()).isEqualTo(4.95);
        assertThat(response.getLocations()).isEqualTo("London, Berlin, New York");
        assertThat(response.getFounded()).isEqualTo(2019);
        assertThat(response.getDescription()).isEqualTo("Modern recruitment platform.");
        assertThat(response.getEmployees()).isEqualTo(800);
        assertThat(response.getWebsite()).isEqualTo("https://jobleaf.example");
        assertThat(response.getCreatedAt()).isEqualTo(createdAt.atOffset(ZoneOffset.UTC));
        assertThat(response.getCreatedBy()).isEqualTo(createdBy);
        assertThat(response.getUpdatedAt()).isEqualTo(updatedAt.atOffset(ZoneOffset.UTC));
        assertThat(response.getUpdatedBy()).isEqualTo(updatedBy);
    }

    @Test
    @DisplayName("Should handle null timestamps gracefully without throwing NPE")
    void toCompanyResponse_withNullTimestamps_handlesNullGracefully() {
        // Given
        Company company = new Company(
                UUID.randomUUID(),
                "No Timestamp Co",
                null,
                "IT",
                "Small",
                3.5,
                null,
                2022,
                null,
                10,
                null,
                null,
                null,
                null,
                null
        );

        // When
        CompanyResponse response = mapper.toCompanyResponse(company);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getCreatedAt()).isNull();
        assertThat(response.getUpdatedAt()).isNull();
        assertThat(response.getCreatedBy()).isNull();
        assertThat(response.getUpdatedBy()).isNull();
    }

    @Test
    @DisplayName("Should map list of domain companies to list of CompanyResponse")
    void toListCompaniesResponse_mapsListOfCompanies() {
        // Given
        Company company1 = new Company(
                UUID.randomUUID(),
                "First Co",
                "/logo1.png",
                "Tech",
                "Small",
                4.0,
                "Loc1",
                2021,
                "Desc1",
                20,
                "https://co1.example",
                Instant.now(),
                UUID.randomUUID(),
                Instant.now(),
                UUID.randomUUID()
        );
        Company company2 = new Company(
                UUID.randomUUID(),
                "Second Co",
                "/logo2.png",
                "Finance",
                "Large",
                4.8,
                "Loc2",
                2015,
                "Desc2",
                500,
                "https://co2.example",
                Instant.now(),
                UUID.randomUUID(),
                Instant.now(),
                UUID.randomUUID()
        );

        // When
        List<CompanyResponse> responses = mapper.toListCompaniesResponse(List.of(company1, company2));

        // Then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getName()).isEqualTo("First Co");
        assertThat(responses.get(1).getName()).isEqualTo("Second Co");
    }
}

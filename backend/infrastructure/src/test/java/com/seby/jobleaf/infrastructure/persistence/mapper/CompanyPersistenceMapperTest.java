package com.seby.jobleaf.infrastructure.persistence.mapper;

import com.seby.jobleaf.domain.model.Company;
import com.seby.jobleaf.infrastructure.persistence.entity.CompanyEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyPersistenceMapperTest {

    private CompanyPersistenceMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new CompanyPersistenceMapper();
    }

    @Test
    @DisplayName("Should map CompanyEntity to domain Company with all properties")
    void toDomain_mapsAllFieldsCorrectly() {
        // Given
        UUID id = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        UUID updatedBy = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-02T12:00:00Z");

        CompanyEntity entity = new CompanyEntity();
        entity.id = id;
        entity.setName("Alpha Solutions");
        entity.setLogo("/logos/alpha.png");
        entity.setIndustry("Consulting");
        entity.setSize("Medium");
        entity.setRating(4.6);
        entity.setLocations("Paris, London");
        entity.setFounded(2012);
        entity.setDescription("Expert IT consulting firm.");
        entity.setEmployees(350);
        entity.setWebsite("https://alpha.example");
        entity.createdAt = createdAt;
        entity.createdBy = createdBy;
        entity.updatedAt = updatedAt;
        entity.updatedBy = updatedBy;

        // When
        Company domain = mapper.toDomain(entity);

        // Then
        assertThat(domain).isNotNull();
        assertThat(domain.id()).isEqualTo(id);
        assertThat(domain.name()).isEqualTo("Alpha Solutions");
        assertThat(domain.logo()).isEqualTo("/logos/alpha.png");
        assertThat(domain.industry()).isEqualTo("Consulting");
        assertThat(domain.size()).isEqualTo("Medium");
        assertThat(domain.rating()).isEqualTo(4.6);
        assertThat(domain.locations()).isEqualTo("Paris, London");
        assertThat(domain.founded()).isEqualTo(2012);
        assertThat(domain.description()).isEqualTo("Expert IT consulting firm.");
        assertThat(domain.employees()).isEqualTo(350);
        assertThat(domain.website()).isEqualTo("https://alpha.example");
        assertThat(domain.createdAt()).isEqualTo(createdAt);
        assertThat(domain.createdBy()).isEqualTo(createdBy);
        assertThat(domain.updatedAt()).isEqualTo(updatedAt);
        assertThat(domain.updatedBy()).isEqualTo(updatedBy);
    }

    @Test
    @DisplayName("Should return null when mapping null Company to CompanyEntity")
    void toEntity_whenCompanyIsNull_returnsNull() {
        // Given
        Company company = null;

        // When
        CompanyEntity entity = mapper.toEntity(company);

        // Then
        assertThat(entity).isNull();
    }

    @Test
    @DisplayName("Should map domain Company to CompanyEntity with all properties")
    void toEntity_mapsAllFieldsCorrectly() {
        // Given
        UUID id = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        UUID updatedBy = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-02T12:00:00Z");

        Company domain = new Company(
                id,
                "Beta Innovations",
                "/logos/beta.png",
                "Biotech",
                "Small",
                4.9,
                "Boston",
                2020,
                "Next-gen biology products.",
                50,
                "https://beta.example",
                createdAt,
                createdBy,
                updatedAt,
                updatedBy
        );

        // When
        CompanyEntity entity = mapper.toEntity(domain);

        // Then
        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getName()).isEqualTo("Beta Innovations");
        assertThat(entity.getLogo()).isEqualTo("/logos/beta.png");
        assertThat(entity.getIndustry()).isEqualTo("Biotech");
        assertThat(entity.getSize()).isEqualTo("Small");
        assertThat(entity.getRating()).isEqualTo(4.9);
        assertThat(entity.getLocations()).isEqualTo("Boston");
        assertThat(entity.getFounded()).isEqualTo(2020);
        assertThat(entity.getDescription()).isEqualTo("Next-gen biology products.");
        assertThat(entity.getEmployees()).isEqualTo(50);
        assertThat(entity.getWebsite()).isEqualTo("https://beta.example");
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(entity.getCreatedBy()).isEqualTo(createdBy);
        assertThat(entity.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(entity.getUpdatedBy()).isEqualTo(updatedBy);
    }
}

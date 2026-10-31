package com.seby.jobleaf.infrastructure.persistence.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyEntityTest {

    @Test
    @DisplayName("Should correctly set and get all fields via setters and getters")
    void shouldSetAndGetAllFields() {
        // Given
        CompanyEntity entity = new CompanyEntity();
        UUID id = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        UUID updaterId = UUID.randomUUID();
        Instant createdAt = Instant.now().minusSeconds(3600);
        Instant updatedAt = Instant.now();

        // When
        entity.id = id;
        entity.createdAt = createdAt;
        entity.createdBy = creatorId;
        entity.updatedAt = updatedAt;
        entity.updatedBy = updaterId;
        entity.setName("Tech Corp");
        entity.setLogo("/logos/techcorp.png");
        entity.setIndustry("Technology");
        entity.setSize("Enterprise");
        entity.setRating(4.9);
        entity.setLocations("New York, London");
        entity.setFounded(2005);
        entity.setDescription("A premier global tech company.");
        entity.setEmployees(25000);
        entity.setWebsite("https://techcorp.example");

        // Then
        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(entity.getCreatedBy()).isEqualTo(creatorId);
        assertThat(entity.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(entity.getUpdatedBy()).isEqualTo(updaterId);
        assertThat(entity.getName()).isEqualTo("Tech Corp");
        assertThat(entity.getLogo()).isEqualTo("/logos/techcorp.png");
        assertThat(entity.getIndustry()).isEqualTo("Technology");
        assertThat(entity.getSize()).isEqualTo("Enterprise");
        assertThat(entity.getRating()).isEqualTo(4.9);
        assertThat(entity.getLocations()).isEqualTo("New York, London");
        assertThat(entity.getFounded()).isEqualTo(2005);
        assertThat(entity.getDescription()).isEqualTo("A premier global tech company.");
        assertThat(entity.getEmployees()).isEqualTo(25000);
        assertThat(entity.getWebsite()).isEqualTo("https://techcorp.example");
    }

    @Test
    @DisplayName("Should instantiate CompanyEntity with all-arguments constructor")
    void shouldInstantiateWithAllArgsConstructor() {
        // Given
        String name = "DerpCorp";
        String logo = "/logos/derpcorp.png";
        String industry = "Software";
        String size = "Medium";
        Double rating = 4.2;
        String locations = "Berlin, Bucharest";
        int founded = 2018;
        String description = "Fast-growing startup";
        Integer employees = 120;
        String website = "https://derpcorp.example";

        // When
        CompanyEntity entity = new CompanyEntity(
                name,
                logo,
                industry,
                size,
                rating,
                locations,
                founded,
                description,
                employees,
                website,
                new ArrayList<>()
        );

        // Then
        assertThat(entity.getName()).isEqualTo(name);
        assertThat(entity.getLogo()).isEqualTo(logo);
        assertThat(entity.getIndustry()).isEqualTo(industry);
        assertThat(entity.getSize()).isEqualTo(size);
        assertThat(entity.getRating()).isEqualTo(rating);
        assertThat(entity.getLocations()).isEqualTo(locations);
        assertThat(entity.getFounded()).isEqualTo(founded);
        assertThat(entity.getDescription()).isEqualTo(description);
        assertThat(entity.getEmployees()).isEqualTo(employees);
        assertThat(entity.getWebsite()).isEqualTo(website);
        assertThat(entity.getJobs()).isEmpty();
    }
}

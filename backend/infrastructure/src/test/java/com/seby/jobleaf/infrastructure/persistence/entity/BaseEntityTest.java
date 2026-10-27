package com.seby.jobleaf.infrastructure.persistence.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BaseEntityTest {

    private static class TestEntity extends BaseEntity {
        public void triggerOnCreate() {
            super.onCreate();
        }

        public void triggerOnUpdate() {
            super.onUpdate();
        }
    }

    @Test
    @DisplayName("Should generate new UUID and createdAt timestamp when id is null on persist")
    void onCreate_whenIdIsNull_generatesNewIdAndTimestamp() {
        // Given
        TestEntity entity = new TestEntity();
        Instant before = Instant.now();

        // When
        entity.triggerOnCreate();
        Instant after = Instant.now();

        // Then
        assertThat(entity.getId()).isNotNull();
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getCreatedAt()).isBetween(before, after);
    }

    @Test
    @DisplayName("Should preserve existing UUID when id is already provided on persist")
    void onCreate_whenIdIsNotNull_preservesExistingId() {
        // Given
        TestEntity entity = new TestEntity();
        UUID predefinedId = UUID.randomUUID();
        entity.id = predefinedId;

        // When
        entity.triggerOnCreate();

        // Then
        assertThat(entity.getId()).isEqualTo(predefinedId);
        assertThat(entity.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should update updatedAt timestamp on update")
    void onUpdate_setsUpdatedAtTimestamp() {
        // Given
        TestEntity entity = new TestEntity();
        Instant before = Instant.now();

        // When
        entity.triggerOnUpdate();
        Instant after = Instant.now();

        // Then
        assertThat(entity.getUpdatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isBetween(before, after);
    }
}

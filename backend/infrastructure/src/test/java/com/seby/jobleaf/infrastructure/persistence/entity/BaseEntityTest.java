package com.seby.jobleaf.infrastructure.persistence.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BaseEntityTest {

    private static class TestEntity extends BaseEntity {
        public void triggerOnCreate() {
            super.onCreate();
        }
    }

    @Test
    @DisplayName("Should generate new UUID when id is null on persist")
    void onCreate_whenIdIsNull_generatesNewId() {
        // Given
        TestEntity entity = new TestEntity();

        // When
        entity.triggerOnCreate();

        // Then
        assertThat(entity.getId()).isNotNull();
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
    }
}

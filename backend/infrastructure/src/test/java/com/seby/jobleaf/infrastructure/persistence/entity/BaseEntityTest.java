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

    @Test
    @DisplayName("Should be new while it has an application-generated id but was never inserted")
    void isNew_whenCreatedAtIsNull_returnsTrue() {
        TestEntity entity = new TestEntity();
        entity.id = UUID.randomUUID();

        assertThat(entity.isNew()).isTrue();
    }

    @Test
    @DisplayName("Should restore the audit values of a persisted record and stop being new")
    void restoreAuditFields_copiesValuesAndMarksNotNew() {
        // Given
        TestEntity entity = new TestEntity();
        Instant createdAt = Instant.parse("2026-10-30T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-10-31T10:00:00Z");
        UUID createdBy = UUID.randomUUID();
        UUID updatedBy = UUID.randomUUID();

        // When
        entity.restoreAuditFields(createdAt, createdBy, updatedAt, updatedBy);

        // Then
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(entity.getCreatedBy()).isEqualTo(createdBy);
        assertThat(entity.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(entity.getUpdatedBy()).isEqualTo(updatedBy);
        assertThat(entity.isNew()).isFalse();
    }

    @Test
    @DisplayName("Should not be new once it has a creation timestamp")
    void isNew_whenCreatedAtIsSet_returnsFalse() {
        TestEntity entity = new TestEntity();
        entity.id = UUID.randomUUID();
        entity.createdAt = Instant.now();

        assertThat(entity.isNew()).isFalse();
    }
}

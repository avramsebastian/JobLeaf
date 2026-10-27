package com.seby.jobleaf.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA abstract class used for auditing.
 */
@Getter
@MappedSuperclass
public abstract class BaseEntity {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    public UUID id;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @Column(name = "created_by", updatable = false)
    public UUID createdBy;

    @Column(name = "updated_at")
    public Instant updatedAt;

    @Column(name = "updated_by")
    public UUID updatedBy;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}

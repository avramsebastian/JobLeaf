package com.seby.jobleaf.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA abstract class used for auditing.
 * <p>
 * {@link Audited} is required so Envers also writes the inherited audit columns into the {@code _aud} tables.
 */
@Getter
@MappedSuperclass
@Audited
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    public UUID id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    public UUID createdBy;

    @LastModifiedDate
    @Column(name = "updated_at")
    public Instant updatedAt;

    @LastModifiedBy
    @Column(name = "updated_by")
    public UUID updatedBy;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}

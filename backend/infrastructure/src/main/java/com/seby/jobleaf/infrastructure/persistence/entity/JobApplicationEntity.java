package com.seby.jobleaf.infrastructure.persistence.entity;

import com.seby.jobleaf.domain.model.jobapplication.ApplicationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.envers.Audited;

import java.time.Instant;

/**
 * JPA entity for the {@code job_applications} table: one job seeker's application to one job.
 * <p>
 * Id and audit fields come from {@link BaseEntity} (UUID, set by the application; auditing fills created/updated).
 * {@code @OnDelete(CASCADE)} tells Hibernate that the database itself deletes the applications of a deleted user or
 * job (the foreign keys are ON DELETE CASCADE), so Hibernate does not try to do it.
 * Cover letter and notes are {@code TEXT} columns; deliberately no {@code @Lob}, which on PostgreSQL would store them
 * as separate large objects.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "job_applications")
@Audited
public class JobApplicationEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private JobLeafUserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "job_id", nullable = false)
    private JobEntity job;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt;

    /** Stored as text ("PENDING"), never as the ordinal, so reordering the enum can not corrupt the data. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ApplicationStatus status;

    @Column(name = "cover_letter", columnDefinition = "TEXT")
    private String coverLetter;

    /** Internal notes of the employer. */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}

package com.seby.jobleaf.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * JPA entity for the {@code users} table: JobLeaf's own user, instead of Spring Security's {@code User}.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
@Audited
public class JobLeafUserEntity extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    /**
     * BCrypt hash. Not audited, so old hashes are not kept in {@code users_aud}.
     */
    @NotAudited
    @Column(name = "password_hash", nullable = false, length = 500)
    private String passwordHash;

    @Column(name = "mobile_number", unique = true, length = 20)
    private String mobileNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleEntity role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private CompanyEntity company;

    /**
     * Jobs this job seeker saved. This side owns the {@code saved_jobs} join table: adding or removing a job here is
     * what inserts or deletes the row. Lazy (the default for collections), so loading a user never loads them.
     * Not audited: Envers would otherwise need a {@code saved_jobs_aud} table for the join rows.
     */
    @NotAudited
    @ManyToMany
    @JoinTable(name = "saved_jobs",
            joinColumns = {@JoinColumn(name = "user_id")},
            inverseJoinColumns = {@JoinColumn(name = "job_id")})
    private Set<JobEntity> savedJobs = new LinkedHashSet<>();
}

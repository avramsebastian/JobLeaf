package com.seby.jobleaf.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

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

    /**
     * Company of an employer; {@code null} for job seekers and admins.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private CompanyEntity company;
}

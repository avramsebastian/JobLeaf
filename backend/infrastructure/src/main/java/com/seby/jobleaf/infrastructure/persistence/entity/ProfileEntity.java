package com.seby.jobleaf.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

/**
 * JPA entity for the {@code profiles} table: one profile per job seeker.
 * <p>
 * The files are plain {@code byte[]} columns, which Hibernate maps to PostgreSQL's {@code bytea}. Deliberately no
 * {@code @Lob}: on PostgreSQL that would store them as separate "large objects" ({@code oid}) instead.
 * The file contents are {@code @NotAudited}, so an update does not copy megabytes into {@code profiles_aud}; the
 * names and types are audited.
 * <p>
 * The relation is one-directional (profile → user). A {@code @OneToOne(mappedBy)} on the user would be loaded eagerly
 * by Hibernate, so every user query (every login) would also load the profile with its files.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "profiles")
@Audited
public class ProfileEntity extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private JobLeafUserEntity user;

    @Column(name = "job_title", nullable = false)
    private String jobTitle;

    @Column(name = "location", nullable = false)
    private String location;

    @Column(name = "experience_level", nullable = false, length = 50)
    private String experienceLevel;

    @Column(name = "professional_bio", nullable = false, columnDefinition = "TEXT")
    private String professionalBio;

    @Column(name = "portfolio_website", length = 500)
    private String portfolioWebsite;

    @NotAudited
    @Column(name = "profile_picture")
    private byte[] profilePicture;

    @Column(name = "profile_picture_name")
    private String profilePictureName;

    @Column(name = "profile_picture_type", length = 100)
    private String profilePictureType;

    @NotAudited
    @Column(name = "resume")
    private byte[] resume;

    @Column(name = "resume_name")
    private String resumeName;

    @Column(name = "resume_type", length = 100)
    private String resumeType;
}

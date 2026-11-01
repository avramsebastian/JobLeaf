package com.seby.jobleaf.infrastructure.persistence.entity;

import com.seby.jobleaf.infrastructure.persistence.converter.StringListJsonConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * JPA entity for the {@code jobs} table.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "jobs")
@Audited
public class JobEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyEntity company;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "location", nullable = false)
    private String location;

    @Column(name = "work_type", nullable = false, length = 50)
    private String workType;

    @Column(name = "job_type", nullable = false, length = 50)
    private String jobType;

    @Column(name = "category", nullable = false, length = 100)
    private String category;

    @Column(name = "experience_level", nullable = false, length = 50)
    private String experienceLevel;

    @Column(name = "salary_min", nullable = false, precision = 12, scale = 2)
    private BigDecimal salaryMin;

    @Column(name = "salary_max", nullable = false, precision = 12, scale = 2)
    private BigDecimal salaryMax;

    @Column(name = "salary_currency", nullable = false, length = 10)
    private String salaryCurrency;

    @Column(name = "salary_period", nullable = false, length = 20)
    private String salaryPeriod;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "requirements", columnDefinition = "TEXT")
    private List<String> requirements;

    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "benefits", columnDefinition = "TEXT")
    private List<String> benefits;

    @Column(name = "posted_date", nullable = false)
    private Instant postedDate;

    @Column(name = "application_deadline")
    private Instant applicationDeadline;

    @Column(name = "applications_count")
    private Integer applicationsCount;

    @Column(name = "featured")
    private boolean featured;

    @Column(name = "urgent")
    private boolean urgent;

    @Column(name = "remote")
    private boolean remote;

    @Column(name = "status", nullable = false, length = 20)
    private String status;
}

package com.seby.jobleaf.infrastructure.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.envers.Audited;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity for the {@code companies} table.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "companies")
@Audited
public class CompanyEntity extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "logo", length = 500)
    private String logo;

    @Column(name = "industry", nullable = false, length = 100)
    private String industry;

    @Column(name = "size", nullable = false, length = 50)
    private String size;

    @Column(name = "rating", nullable = false)
    private Double rating;

    @Column(name = "locations", length = 1000)
    private String locations;

    @Column(name = "founded", nullable = false)
    private int founded;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "employees")
    private Integer employees;

    @Column(name = "website", length = 500)
    private String website;

    /**
     * Lazily loaded; {@link BatchSize} loads the jobs of several companies per query to avoid N+1 selects.
     */
    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private List<JobEntity> jobs = new ArrayList<>();
}

package com.seby.jobleaf.infrastructure.persistence.repository;

import com.seby.jobleaf.infrastructure.persistence.entity.CompanyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CompanyJpaRepository extends JpaRepository<CompanyEntity, UUID> {

    @Query("SELECT distinct c from CompanyEntity c Join fetch c.jobs j where j.status=:status")
    List<CompanyEntity> findAllWithJobsByStatus(@Param("status")String status);

    /**
     * Derived query: {@code WHERE lower(name) = lower(?)}, used to keep company names unique for the admin.
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Same as {@link #existsByNameIgnoreCase} but ignores one company: renaming a company to its own name is fine.
     */
    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
}

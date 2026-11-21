package com.seby.jobleaf.infrastructure.persistence.repository;

import com.seby.jobleaf.infrastructure.persistence.entity.JobEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JobJpaRepository extends JpaRepository<JobEntity, UUID> {

    /**
     * Derived query: {@code company_Id} walks into the company relation, so this is
     * {@code WHERE company_id = ? ORDER BY created_at DESC}.
     */
    List<JobEntity> findByCompany_IdOrderByCreatedAtDesc(UUID companyId);
}

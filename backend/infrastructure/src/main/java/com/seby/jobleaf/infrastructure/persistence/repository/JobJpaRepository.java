package com.seby.jobleaf.infrastructure.persistence.repository;

import com.seby.jobleaf.infrastructure.persistence.entity.JobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface JobJpaRepository extends JpaRepository<JobEntity, UUID> {

    /**
     * Derived query: {@code company_Id} walks into the company relation, so this is
     * {@code WHERE company_id = ? ORDER BY created_at DESC}.
     */
    List<JobEntity> findByCompany_IdOrderByCreatedAtDesc(UUID companyId);

    /**
     * {@code join u.savedJobs} goes through the {@code saved_jobs} join table of the many-to-many relation:
     * SQL joins users → saved_jobs → jobs.
     */
    @Query("select j from JobLeafUserEntity u join u.savedJobs j where u.id = :userId order by j.postedDate desc")
    List<JobEntity> findSavedByUserId(@Param("userId") UUID userId);

    /**
     * Whether the row (user, job) exists in {@code saved_jobs}, without loading the user's whole list.
     */
    @Query("""
            select case when count(j) > 0 then true else false end
            from JobLeafUserEntity u join u.savedJobs j
            where u.id = :userId and j.id = :jobId""")
    boolean isSavedByUser(@Param("userId") UUID userId, @Param("jobId") UUID jobId);
}

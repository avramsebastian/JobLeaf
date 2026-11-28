package com.seby.jobleaf.infrastructure.persistence.repository;

import com.seby.jobleaf.domain.model.jobapplication.ApplicationStatus;
import com.seby.jobleaf.infrastructure.persistence.entity.JobApplicationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobApplicationJpaRepository extends JpaRepository<JobApplicationEntity, UUID> {

    /**
     * {@code join fetch a.job} loads the job in the same query: the response always shows the job, and without it
     * every application would cost one extra SELECT for its job.
     */
    @Query("select a from JobApplicationEntity a join fetch a.job where a.user.id = :userId and a.job.id = :jobId")
    Optional<JobApplicationEntity> findByUserAndJob(@Param("userId") UUID userId, @Param("jobId") UUID jobId);

    @Query("select a from JobApplicationEntity a join fetch a.job where a.user.id = :userId order by a.appliedAt desc")
    List<JobApplicationEntity> findAllByUser(@Param("userId") UUID userId);

    /**
     * For the employer: the applicant (user) is loaded in the same query as well.
     */
    @Query("""
            select a from JobApplicationEntity a join fetch a.job join fetch a.user
            where a.job.id = :jobId and a.status <> :excluded
            order by a.appliedAt desc""")
    List<JobApplicationEntity> findAllByJobExcept(@Param("jobId") UUID jobId,
                                                  @Param("excluded") ApplicationStatus excluded);

    @Query("select a from JobApplicationEntity a join fetch a.job join fetch a.user where a.id = :id")
    Optional<JobApplicationEntity> findWithJobAndUserById(@Param("id") UUID id);
}

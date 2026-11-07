package com.seby.jobleaf.infrastructure.persistence.repository;

import com.seby.jobleaf.infrastructure.persistence.entity.JobLeafUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface JobLeafUserJpaRepository extends JpaRepository<JobLeafUserEntity, UUID> {

    /**
     * Loads the user with role and company in one query, so they can be read outside a transaction
     * (e.g. by the authentication provider).
     */
    @Query("select u from JobLeafUserEntity u join fetch u.role left join fetch u.company where u.email = :email")
    Optional<JobLeafUserEntity> findWithRoleByEmail(@Param("email") String email);

    boolean existsByEmail(String email);

    boolean existsByMobileNumber(String mobileNumber);
}

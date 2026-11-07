package com.seby.jobleaf.infrastructure.persistence.repository;

import com.seby.jobleaf.domain.model.RoleName;
import com.seby.jobleaf.infrastructure.persistence.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoleJpaRepository extends JpaRepository<RoleEntity, UUID> {

    Optional<RoleEntity> findByName(RoleName name);
}

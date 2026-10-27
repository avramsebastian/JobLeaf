package com.seby.jobleaf.infrastructure.persistence.repository;

import com.seby.jobleaf.infrastructure.persistence.entity.CompanyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CompanyJpaRepository extends JpaRepository<CompanyEntity, UUID> {
}

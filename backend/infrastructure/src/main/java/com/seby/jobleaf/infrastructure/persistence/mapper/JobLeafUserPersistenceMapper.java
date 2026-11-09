package com.seby.jobleaf.infrastructure.persistence.mapper;

import com.seby.jobleaf.domain.model.JobLeafUser;
import com.seby.jobleaf.infrastructure.persistence.entity.JobLeafUserEntity;
import org.springframework.stereotype.Component;

@Component
public class JobLeafUserPersistenceMapper {

    /**
     * The password hash is deliberately not mapped: it never leaves the persistence and security adapters.
     */
    public JobLeafUser toDomain(JobLeafUserEntity entity) {
        if (entity == null) {
            return null;
        }
        return new JobLeafUser(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getMobileNumber(),
                entity.getRole().getName(),
                entity.getCompany() == null ? null : entity.getCompany().getId(),
                entity.getCompany() == null ? null : entity.getCompany().getName(),
                entity.getCreatedAt()
        );
    }
}
